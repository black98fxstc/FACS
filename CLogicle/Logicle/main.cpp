#include <iostream>
#include "logicle.h"

using namespace std;

int main(int argc, char **argv, char **envp)
{
  /* This could parse the commanline arguments and create a logicle based on that */

  double T = 10000.0;
  double W = 0.5;
  double M = 4.5;
  double A = 0.0;

  Logicle *l = (Logicle *)logicle_initialize(T, W, M, A, 0);

  double value = 1234.0;
  double result = l->scale(value);

  cout << "The result of Logicle (T=" << T << ",W=" << W << ",M=" << M << ",A=" << A << ") applied on " << value << " is " << result << "." << endl;
 
  return 0;    
}
