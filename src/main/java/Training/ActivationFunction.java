package Training;

import java.io.Serializable;

public abstract class ActivationFunction implements Serializable {
    /**
     * Abstract method to activate a given input.
     *
     * @param x the input value to be activated
     * @return the activated value
     */
    abstract public float activate(float x);

    /**
     * Abstract method to compute the derivative of the activation function for a given input.
     *
     * @param x the input value for which the derivative is to be computed
     * @return the derivative of the activation function at the given input
     */
    abstract public float derivative(float x);
}
