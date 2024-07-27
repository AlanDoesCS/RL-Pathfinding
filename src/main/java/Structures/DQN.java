package Structures;

import Training.ActivationFunction;

import java.util.List;

public class DQN extends NN {
    private final int inputSize, outputSize;

    public DQN(int inputSize, List<Layer> hiddenLayers, int outputSize, float learningRate, ActivationFunction outputActivation, float outputBias) {
        this.hiddenLayers  = hiddenLayers;
        this.outputLayer = new Layer(hiddenLayers.getLast().getOutputSize(), outputSize, outputActivation, outputBias);
        this.learningRate = learningRate;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public Matrix getOutput(Matrix input) {
        for (Layer layer : hiddenLayers) {
            input = layer.compute(input);
        }
        return outputLayer.compute(input); // is output after all layers
    }

    public void addLayer(int size, ActivationFunction phi, float bias) {
        hiddenLayers.add(new Layer(hiddenLayers.getLast().getOutputSize(), size, phi, bias));
    }



    /*
    -----------------------------------------------------------------------------

    ACCESSORS AND MUTATORS

    -----------------------------------------------------------------------------
    */
    public int layers() {
        return hiddenLayers.size()+2; //input + hidden + output
    }

    public int getInputSize() {
        return inputSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public Layer getHiddenLayer(int i) {
        return hiddenLayers.get(i);
    }

    public Layer getOutputLayer() {
        return outputLayer;
    }
}
