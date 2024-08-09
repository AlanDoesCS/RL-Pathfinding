package Structures;

import java.io.Serializable;
import java.util.List;

public abstract class Layer implements Serializable {
    protected int inputSize;
    protected int outputSize;

    public abstract Matrix compute(Matrix input);
    public abstract String toString();
    public int getOutputSize() {
        return outputSize;
    }

    public int getInputSize() {
        return inputSize;
    }

    public static String toString(List<Layer> layers) {
        StringBuilder sb = new StringBuilder();
        for (Layer layer : layers) {
            sb.append(layer.toString()).append("\n");
        }
        return sb.toString();
    }

    public abstract Matrix backpropagate(Matrix input, Matrix gradientOutput);
    public abstract void updateParameters(float learningRate);
}
