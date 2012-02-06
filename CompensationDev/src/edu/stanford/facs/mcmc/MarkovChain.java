package edu.stanford.facs.mcmc;

public class MarkovChain
{

  public static double run (int steps, State state, double logStep)
  {
    int count = 0;
    for (int i = 0; i < steps; ++i)
    {
      State proposed = state.propose(logStep);
      
      double acceptance = Math.min(1, Math.exp(proposed.lnP - state.lnP)
        * proposed.transitionRatio);
      if (Math.random() < acceptance)
      {
        state.accept();
        ++count;
      }
      else
        state.reject();
    }
    return (double)count / (double)steps;
  }
}
