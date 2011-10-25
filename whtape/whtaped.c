
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <syslog.h>

#include <errno.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/poll.h>
#include <sys/mtio.h>
#include <sys/wait.h>

#define MAX_BUFFER  16384
#define MAX_BUFFERS  16

#define VOL1  1
#define HDR1  2
#define HDR2  3
#define HDR3  4
#define TM1   5
#define DATA  6
#define EOF1  7
#define EOF2  8
#define EOF3  9
#define TM2   10
#define EOT   11

#define WH_LOADED   1
#define WH_CONTINUE  2
#define WH_ERROR    3

typedef char tape_label[80];
typedef char file_path[1024];

#define DEBUG  0
#if DEBUG
#define dbg_printf(text) printf(text)
#define dbg_printf1(text,arg) printf(text,arg)
#define dbg_printf2(text,arg1,arg2) printf(text,arg1,arg2)
#define dbg_perror(text) perror(text)
#else
#define dbg_printf(text)
#define dbg_printf1(text,arg)
#define dbg_printf2(text,arg1,arg2)
#define dbg_perror(text)
#endif

struct mtop variable_block = { MTSETBLK, 0 };
struct mtop load_tape = { MTLOAD, 0 };
struct mtop rewind_tape = { MTREW, 0 };
struct mtop eject_tape = { MTOFFL, 0 };
file_path  whouse_dir, whtape_dir;

struct mtget  tape_status;

struct stat file_stat;

struct buffer
{
  int count;
  char data[MAX_BUFFER];
};
struct buffer buffers[MAX_BUFFERS], *buf;

int tape_fd;
char file_name[80], tape_volume[8], tape_dev[80] = "/dev/st0";
char edesk_root[256] = "/var/EDesk", edesk_site[256];

int read_tape(  )
{
  file_path file_name, temp_name, temp_dir, tape_dir;
  int file_count = 0, tape_buf = 0, disk_buf = 0, buffered = 0;
  char block_str[7], seq_str[5];
  tape_label vol1, hdr1, hdr2, hdr3, eof1, eof2, eof3;
  int state = VOL1;
  int block, sequence = 0, file_total, tape_total = 0;
  int disk_fd;
  struct pollfd poll_fds[2];
  struct pollfd *poll_tape = poll_fds;
  struct pollfd *poll_disk = poll_fds + 1;

  int sts, fn_length;
  char *fn;

  poll_tape->fd = tape_fd;
   poll_tape->events = POLLIN;
  poll_tape->revents = 0;
  poll_disk->events = POLLOUT;
   poll_disk->revents = POLLOUT;

  while ( state != EOT )
  {
    if ( state == DATA )
      sts = poll( poll_fds, 2, 0 );
    else
      sts = poll( poll_fds, 1, 0 );
    if ( sts < 0 )
    {
      syslog( LOG_ERR, "WHtape poll %m" );
      dbg_perror( "WHtape poll" );
      exit( errno );
    }
    else if ( sts == 0 )
      continue;

    if ( buffered > 0 )
      dbg_printf1( "\007%d buffers\n", buffered );

    if ( poll_tape->revents & ( POLLERR | POLLHUP | POLLNVAL ) )
    {
      syslog( LOG_ERR, "WHtape tape error" );
      dbg_printf( "WHtape tape error\n" );
      exit( EXIT_FAILURE );
    }
    if ( poll_disk->revents & ( POLLERR | POLLHUP | POLLNVAL ) )
    {
      syslog( LOG_ERR, "WHtape disk error" );
      dbg_printf( "WHtape disk error\n" );
      exit( EXIT_FAILURE );
    }

    if ( poll_tape->revents & POLLIN && buffered < MAX_BUFFERS )
    {
      buf = buffers + tape_buf++;
      if ( tape_buf == MAX_BUFFERS )
        tape_buf = 0;
      buffered++;

      buf->count = read( tape_fd, buf->data, MAX_BUFFER );
      if ( buf->count < 0 )
      {
        syslog( LOG_ERR, "WHtape read tape %m" );
        dbg_perror( "WHtape read tape" );
          return  WH_ERROR;
      }
    }

    if ( poll_disk->revents & POLLOUT && buffered > 0 )
    {
      buf = buffers + disk_buf++;
      if ( disk_buf == MAX_BUFFERS )
        disk_buf = 0;
      buffered--;

      switch ( state )
      {
      case VOL1:
        if ( buf->count != 80 || memcmp( buf->data, "VOL1", 4 )
             || memcmp( buf->data + 10,
                        "                           EDESK WHOUSE                              3",
                        70 ) )
        {
          syslog( LOG_NOTICE, "unrecognized tape loaded" );
          dbg_printf( "WHtape malformed VOL1\n" );
          return WH_CONTINUE;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( vol1, buf->data, 80 );

        sprintf( tape_volume, "%.5s", vol1 + 4 );
        sprintf( tape_dir, "%s/%s", whouse_dir, tape_volume );
        sts = stat( tape_dir, &file_stat );
        if ( sts == 0 )
        {
          syslog( LOG_NOTICE, "%s loaded but not needed", tape_volume );
          dbg_printf1( "WHtape %s is not needed\n", tape_volume );
          return WH_CONTINUE;
        }
        else
        {
          syslog( LOG_NOTICE, "%s loaded", tape_volume );
          dbg_printf1( "WHtape %s loaded\n", tape_volume );
        }

        sprintf( temp_dir, "%s/%s", whtape_dir, tape_volume );
        sts = mkdir( temp_dir, 0777 );
        if ( sts < 0 && errno != EEXIST)
        {
          syslog( LOG_ERR, "WHtape directory error" );
          dbg_perror( "WHtape directory error" );
          exit( errno );
        }

        sprintf( file_name, "%s/CSDATA",temp_dir );
        sts = mkdir( file_name, 0777 );
        if ( sts < 0 && errno != EEXIST)
        {
          syslog( LOG_ERR, "WHtape directory error" );
          dbg_perror( "WHtape directory error" );
          exit( errno );
        }

        sprintf( file_name, "%s/CSAMPL", temp_dir );
        sts = mkdir( file_name, 0777 );
        if ( sts < 0 && errno != EEXIST)
        {
          syslog( LOG_ERR, "WHtape directory error" );
          dbg_perror( "WHtape directory error" );
          exit( errno );
        }

        sprintf( temp_name, "%s/whtaped.tmp", temp_dir );

        state = HDR1;
        break;

      case HDR1:
        block = 0;
        file_total = 0;
        sprintf( block_str, "%06d", block );
        sprintf( seq_str, "%04d", ++sequence );
        if ( buf->count == 0 )
        {
          dbg_printf( "********** TAPE MARK **********\n" );
          dbg_printf( "********* END OF TAPE *********\n" );
          state = EOT;
          break;
        }
        else if ( buf->count != 80 || memcmp( buf->data, "HDR1", 4 )
                  || memcmp( buf->data + 21, vol1 + 4, 6 )
                  || memcmp( buf->data + 27, "0001", 4 )
                  || memcmp( buf->data + 31, seq_str, 4 )
                  || memcmp( buf->data + 35, "000100 00000 99365 ",
                             19 )
                  || memcmp( buf->data + 54, block_str, 6 )
                  || memcmp( buf->data + 60, "WHOUSE V1           ", 20 ) )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape malformed HDR1\n" );
          return  WH_ERROR;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( hdr1, buf->data, 80 );
        state = HDR2;
        break;

      case HDR2:
        if ( buf->count != 80
             || memcmp( buf->data,
                        "HDR2F1638400512                     M             00                            ",
                        80 ) )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape malformed HDR2\n" );
          return  WH_ERROR;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( hdr2, buf->data, 80 );
        state = HDR3;
        break;

      case HDR3:
        if ( buf->count != 80 || memcmp( buf->data, "HDR3", 4 ) )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape malformed HDR3\n" );
          return  WH_ERROR;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( hdr3, buf->data, 80 );

        for ( fn = hdr1 + 4, fn_length = 0; fn_length < 17; fn_length++ )
          if ( fn[fn_length] == ' ' )
            break;
        sprintf( file_name, "%s/%.6s/%.*s", temp_dir, hdr3 + 4, fn_length, fn );
        poll_disk->fd = disk_fd =
          open( temp_name,
                O_CREAT | O_TRUNC | O_WRONLY | O_NONBLOCK,
                S_IRUSR | S_IWUSR );
        if ( disk_fd < 0 )
        {
          syslog( LOG_ERR, "WHtape open data file %m" );
          dbg_perror( "WHtape open data file" );
          exit( errno );
        }

        state = TM1;
        break;

      case TM1:
        if ( buf->count != 0 )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape missing tapemark\n" );
          return  WH_ERROR;
        }
        dbg_printf( "********** TAPE MARK **********\n" );
        state = DATA;
        break;

      case DATA:
        if ( buf->count > 0 )
        {
          sts = write( disk_fd, buf->data, buf->count );
          if ( sts < 0 )
          {
            syslog( LOG_ERR, "WHtape disk write %m" );
            dbg_perror( "WHtape disk write" );
            exit( errno );
          }
          if ( sts != buf->count )
          {
            syslog( LOG_ERR, "WHtape disk write incomplete" );
            dbg_printf( "WHtape disk write incomplete\n" );
            exit( EXIT_FAILURE );
          }
          ++block;
          file_total += buf->count;
        }
        else
        {
          sts = close( disk_fd );
          if ( sts < 0 )
          {
            syslog( LOG_ERR, "WHtape disk close %m" );
            dbg_perror( "WHtape disk close" );
            exit( errno );
          }
          tape_total += file_total;
          ++file_count;

          sts = rename( temp_name, file_name );
          if ( sts != 0 )
          {
             syslog( LOG_ERR, "WHtape rename data" );
            dbg_perror( "WHtape rename data" );
            exit( EXIT_FAILURE );
          }

          sts = chmod( file_name, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH );
          if ( sts != 0 )
          {
             syslog( LOG_ERR, "WHtape chmod" );
            dbg_perror( "WHtape chmod" );
            exit( EXIT_FAILURE );
          }

          dbg_printf1( "#### %10d bytes of data ####\n", file_total );
          dbg_printf( "********** TAPE MARK **********\n" );
          state = EOF1;
        }
        break;

      case EOF1:
        sprintf( block_str, "%06d", block );
        if ( buf->count != 80 || memcmp( buf->data, "EOF1", 4 )
             || memcmp( buf->data + 4, hdr1 + 4, 50 )
             || memcmp( buf->data + 54, block_str, 6 )
             || memcmp( buf->data + 60, hdr1 + 60, 20 ) )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape malformed EOF1\n" );
          return  WH_ERROR;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( hdr1, buf->data, 80 );
        state = EOF2;
        break;

      case EOF2:
        if ( buf->count != 80 || memcmp( buf->data, "EOF2", 4 )
             || memcmp( buf->data + 4, hdr2 + 4, 76 ) )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape malformed EOF2\n" );
          return  WH_ERROR;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( hdr2, buf->data, 80 );
        state = EOF3;
        break;

      case EOF3:
        if ( buf->count != 80 || memcmp( buf->data, "EOF3", 4 )
             || memcmp( buf->data + 4, hdr3 + 4, 76 ) )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape malformed EOF3\n" );
          return  WH_ERROR;
        }
        dbg_printf1( "%.80s\n", buf->data );
        memcpy( hdr3, buf->data, 80 );
        state = TM2;
        break;

      case TM2:
        if ( buf->count != 0 )
        {
          syslog( LOG_ERR, "WHtape malformed" );
          dbg_printf( "WHtape missing tapemark\n" );
          return  WH_ERROR;
        }
        dbg_printf( "********** TAPE MARK **********\n" );
        state = HDR1;
        break;
      }
    }
  }

  syslog( LOG_NOTICE, " %d files, %d bytes of data in loaded", file_count, tape_total );
  dbg_printf1( "%10d bytes of data loaded\n", tape_total );

  sts = rename( temp_dir, tape_dir );
  if ( sts != 0 )
  {
    syslog( LOG_ERR, "WHtape rename directory" );
    dbg_perror( "WHtape rename directory" );
    exit( EXIT_FAILURE );
  }

  return  WH_LOADED;
}

int main( int argc, char *argv[], char *envp[] )
{
  int sts, read_result, log_fd;
  pid_t pid;
  char *str;
  char java_bin[256] = "/usr/bin/java";
  char java_prop[256], jar_file[256];
  char *java_argv[40], **java_argp = java_argv;

  str = getenv( "EDESK_ROOT" );
  if (str != NULL)
    strcpy( edesk_root, str );
  str = getenv( "EDESK_SITE" );
  if (str != NULL)
    strcpy( edesk_site, str );
  if (argc > 1)
    strcpy( edesk_site, argv[1] );
  str = getenv( "WHOUSE_TAPE" );
  if (str != NULL)
    strcpy( tape_dev, str );

  str = getenv( "JAVA_HOME" );
  if (str != NULL)
    sprintf( java_bin, "%s/bin/java", str );
  sprintf( java_prop, "-Dedesk.root=%s", edesk_root );
  sprintf( jar_file, "%s/EDesk.jar", edesk_root );

  *java_argp++ = java_bin;
  *java_argp++ = java_prop;
  *java_argp++ = "-cp";
  *java_argp++ = jar_file;
  *java_argp++ = "edu.stanford.facs.desk.Warehouse";
  *java_argp++ = edesk_site;
  *java_argp++ = tape_volume;
  *java_argp++ = NULL;


  tape_fd = open( tape_dev, O_RDONLY | O_NONBLOCK );
  if ( tape_fd < 0 )
  {
    dbg_perror( "WHtape open tape" );
    exit( errno );
   }
  sts = ioctl( tape_fd, MTIOCTOP, &eject_tape );


  sprintf( whouse_dir, "%s/%s/WHouse", edesk_root, edesk_site );
  sts = mkdir( whouse_dir, 0777 );
  if (sts < 0 && errno != EEXIST)
  {
    dbg_perror( "WHtape whouse mkdir" );
    dbg_printf1( "%s", whouse_dir );
    exit( errno );
  }

  sprintf( whtape_dir, "%s/%s/WHtape", edesk_root, edesk_site );
  sts = mkdir( whtape_dir, 0777 );
  if (sts < 0 && errno != EEXIST)
  {
    dbg_perror( "WHtape whtape mkdir" );
    exit( errno );
  }

#if DEBUG==0
  pid = fork(  );
  if ( pid < 0 )
  {
    dbg_perror( "WHtape fork" );
    exit( errno );
  }
  else if ( pid > 0 )
    return EXIT_SUCCESS;
#endif

  openlog( "whouse", 0, LOG_LOCAL0 );
  syslog( LOG_NOTICE, "root is %s site is %s", edesk_root, edesk_site );
  dbg_printf2( "WHouse root is %s site is %s\n", edesk_root, edesk_site );
  
#if DEBUG==0
  pid = setsid(  );
  if ( pid < 0 )
  {
    syslog( LOG_ERR, "WHtape setsid %m" );
    dbg_perror( "WHtape setsid" );
    exit( errno );
  }

  sprintf( file_name, "%s/%s/whtaped.out", edesk_root, edesk_site );
  log_fd = creat( file_name, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH );
  if ( log_fd < 0 )
  {
    syslog( LOG_ERR, "WHtape create log file %m" );
    dbg_perror( "WHtape create log file" );
    exit( errno );
  }
  sts = dup2( log_fd, 1 );
  if ( sts < 0 )
  {
    syslog( LOG_ERR, "WHtape dup stdout %m" );
    dbg_perror( "WHtape dup stdout" );
    exit( errno );
  }
  sts = dup2( 1, 2 );
  if ( sts < 0 )
  {
    syslog( LOG_ERR, "WHtape dup stderr %m" );
    dbg_perror( "WHtape dup stderr" );
    exit( errno );
  }
  close( log_fd );
#endif

  for ( ;; )
  {
    for ( ;; )
    {
      sts = close( tape_fd );

#if DEBUG==0
      sleep( 10 );
#else
      sleep( 1 );
#endif

      tape_fd = open( tape_dev, O_RDONLY | O_NONBLOCK );
      if ( tape_fd < 0 )
      {
        syslog( LOG_ERR, "WHtape open tape %m" );
        dbg_perror( "WHtape open tape" );
        exit( errno );
      }
    
      sts = ioctl( tape_fd, MTIOCGET, &tape_status );
      if ( sts < 0 )
      {
        syslog( LOG_ERR, "WHtape tape status %m" );
        dbg_perror( "WHtape tape status" );
        exit( errno );
      }
      if (GMT_DR_OPEN(tape_status.mt_gstat))
        continue;
      
      sts = ioctl( tape_fd, MTIOCTOP, &load_tape );
      if ( sts < 0 )
      {
        dbg_perror( "WHtape load" );
        continue;
      }

      sts = ioctl( tape_fd, MTIOCTOP, &variable_block );
      if ( sts < 0 )
      {
        syslog( LOG_ERR, "WHtape set variable block %m" );
        dbg_perror( "WHtape set variable block" );
        exit( errno );
      }
      break;
    }

    sts = read_result = read_tape(  );
    switch (sts)
    {
    case WH_LOADED:
      pid = fork(  );
        if ( pid < 0 )
      {
        syslog( LOG_ERR, "WHtape fork %m" );
          dbg_perror( "WHtape fork" );
        exit( errno );
      }
      else if ( pid == 0 )
      {
        dbg_printf( "Hellow World! (Briefly)\n" );
     
        sts = execve( java_bin, java_argv, envp );
        syslog( LOG_ERR, "WHtape execve %m" );
        dbg_perror( "WHtape execve" );
        exit( errno );
      }
      else
      {
        pid = waitpid( pid, &sts, 0 );
        if ( pid < 0 )
        {
          syslog( LOG_ERR, "WHtape wait %m" );
          dbg_perror( "WHtape wait" );
          exit( errno );
        }
        else if (WIFEXITED(sts))
        {
          sts = WEXITSTATUS(sts);
          dbg_printf1( "WHtape child returned status %d\n", sts );
          if (sts != 0)
          {
            syslog( LOG_ERR, "WHtape child returned status %d", sts );
            exit( EXIT_FAILURE );
          }
        }
        else if (WIFSIGNALED(sts))
        {
          sts = WTERMSIG(sts);
          if (sts != 0)
            syslog( LOG_ERR, "WHtape child signaled %d", sts );
          dbg_printf1( "WHtape child signaled %d\n", sts );
          exit( EXIT_FAILURE );
        }
        else
        {
          syslog( LOG_ERR, "WHtape child ended badly" );
          dbg_printf( "WHtape child ended badly\n" );
          exit( EXIT_FAILURE );
        }                            
      }
      break;

    case WH_ERROR:
      syslog( LOG_NOTICE, "tape daemon recovered from error, continuing" );
      dbg_printf( "WHtape daemon recovered from  error and is continuing\n" );

    case WH_CONTINUE:
      ;
    }

    sts = ioctl( tape_fd, MTIOCTOP, &eject_tape );
    if ( sts < 0 )
    {
      syslog( LOG_ERR, "WHtape rewind %m" );
      dbg_perror( "WHtape rewind" );
      exit( errno );
    }
  }
  return EXIT_SUCCESS;
}
