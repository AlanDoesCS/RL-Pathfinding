package Structures;

/**
 * Represents a Batch Normalization layer in a neural network.
 * <p>
 * This class extends the abstract Layer class and provides functionality
 * for normalizing the inputs to a layer, which helps in accelerating the
 * training process and improving the performance of the neural network.
 * </p>
 */
public class BatchNormLayer extends Layer {
    private int depth, height, width;
    private double epsilon = 1e-5f;
    private double momentum = 0.99f;

    public double[] gamma;
    public double[] beta;
    private double[] runningMean;
    private double[] runningVar;

    private double[] dGamma;
    private double[] dBeta;

    // [2][depth]
    public double[][] m; // First moment estimates for gamma and beta
    public double[][] v; // Second moment estimates for gamma and beta

    public BatchNormLayer(int depth, int height, int width) {
        this.depth = depth;
        this.height = height;
        this.width = width;
        this.inputSize = depth * height * width;
        this.outputSize = inputSize;

        gamma = new double[depth];
        beta = new double[depth];
        runningMean = new double[depth];
        runningVar = new double[depth];
        dGamma = new double[depth];
        dBeta = new double[depth];

        for (int i = 0; i < depth; i++) {
            gamma[i] = 1.0f;
            beta[i] = 0.0f;
        }

        m = new double[2][depth];
        v = new double[2][depth];
    }

    @Override
    public Object compute(Object input) {
        if (input instanceof Tensor inputTensor) {
            return computeTensor(inputTensor);
        } else if (input instanceof MatrixDouble inputMatrix) {
            return computeMatrix(inputMatrix);
        } else {
            throw new IllegalArgumentException("Expected input to be a Tensor or MatrixDouble.");
        }
    }

    private Tensor computeTensor(Tensor inputTensor) {
        Tensor outputTensor = new Tensor(depth, height, width);

        for (int d = 0; d < depth; d++) {
            double mean = 0, variance = 0;
            int count = height * width;

            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    mean += inputTensor.get(d, h, w);
                }
            }
            mean /= count;

            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    double diff = inputTensor.get(d, h, w) - mean;
                    variance += diff * diff;
                }
            }
            variance /= count;

            runningMean[d] = momentum * runningMean[d] + (1 - momentum) * mean;
            runningVar[d] = momentum * runningVar[d] + (1 - momentum) * variance;

            // Normalize and scale
            double stdDev = Math.sqrt(variance + epsilon);
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    double normalized = (inputTensor.get(d, h, w) - mean) / stdDev;
                    outputTensor.set(d, h, w, gamma[d] * normalized + beta[d]);
                }
            }
        }

        return outputTensor;
    }

    private MatrixDouble computeMatrix(MatrixDouble inputMatrix) {
        int rows = inputMatrix.getRows();
        int cols = inputMatrix.getCols();

        MatrixDouble outputMatrix = new MatrixDouble(rows, cols);
        double mean = inputMatrix.getMeanAverage();
        double variance = inputMatrix.getVariance(mean);

        double stdDev = Math.sqrt(variance + epsilon);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double normalized = (inputMatrix.get(j, i) - mean) / stdDev;
                outputMatrix.set(j, i, gamma[0] * normalized + beta[0]);
            }
        }

        return outputMatrix;
    }

    @Override
    public Object backpropagate(Object input, Object gradientOutput) {
        if (input instanceof Tensor inputTensor && gradientOutput instanceof Tensor gradOutputTensor) {
            return backpropagateTensor(inputTensor, gradOutputTensor);
        } else if (input instanceof MatrixDouble inputMatrix && gradientOutput instanceof MatrixDouble gradOutputMatrix) {
            return backpropagateMatrix(inputMatrix, gradOutputMatrix);
        } else {
            throw new IllegalArgumentException("Expected input and gradientOutput to be a Tensor or MatrixDouble.");
        }
    }

    private Tensor backpropagateTensor(Tensor inputTensor, Tensor gradOutputTensor) {
        int N = inputTensor.getHeight() * inputTensor.getWidth();
        Tensor gradInputTensor = new Tensor(depth, height, width);

        for (int d = 0; d < depth; d++) {
            double mean = runningMean[d];
            double variance = runningVar[d];
            double stdDev = Math.sqrt(variance + epsilon);

            double dMean = 0, dVar = 0;

            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    dMean += gradOutputTensor.get(d, h, w);
                    dVar += (inputTensor.get(d, h, w) - mean) * gradOutputTensor.get(d, h, w);
                }
            }

            dMean /= N;
            dVar *= -0.5 / (variance + epsilon);

            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    double input = inputTensor.get(d, h, w);
                    double grad = gradOutputTensor.get(d, h, w);
                    double dInput = grad / stdDev + dVar * 2 * (input - mean) / N + dMean;
                    gradInputTensor.set(d, h, w, dInput);
                }
            }

            dGamma[d] += gradOutputTensor.getSum() / stdDev;
            dBeta[d] += gradOutputTensor.getSum();
        }

        return gradInputTensor;
    }

    private MatrixDouble backpropagateMatrix(MatrixDouble inputMatrix, MatrixDouble gradOutputMatrix) {
        int rows = inputMatrix.getRows();
        int cols = inputMatrix.getCols();

        MatrixDouble gradInputMatrix = new MatrixDouble(rows, cols);
        double mean = inputMatrix.getMeanAverage();
        double variance = inputMatrix.getVariance(mean);
        double stdDev = Math.sqrt(variance + epsilon);

        MatrixDouble inputMinusMean = MatrixDouble.subtract(inputMatrix, mean);
        MatrixDouble multipliedMatrix = MatrixDouble.multiply(gradOutputMatrix, inputMinusMean.transpose());
        double dVar = -0.5 / (variance + epsilon) * multipliedMatrix.getSum();

        double dMean = gradOutputMatrix.getSum() / (rows * cols);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double input = inputMatrix.get(j, i);
                double grad = gradOutputMatrix.get(j, i);
                double dInput = grad / stdDev + dVar * 2 * (input - mean) / (rows * cols) + dMean;
                gradInputMatrix.set(j, i, dInput);
            }
        }

        // Update dGamma and dBeta with the sum of gradients
        dGamma[0] += gradOutputMatrix.getSum() / stdDev;
        dBeta[0] += gradOutputMatrix.getSum();

        return gradInputMatrix;
    }

    @Override
    public void updateParameters(double learningRate) {
        for (int d = 0; d < depth; d++) {
            // Update gamma and beta
            gamma[d] -= learningRate * dGamma[d];
            beta[d] -= learningRate * dBeta[d];
            // Reset gradients after update
            dGamma[d] = 0;
            dBeta[d] = 0;
        }
    }

    @Override
    public void copyTo(Layer targetLayer, boolean ignorePrimitives) {
        // Ensure target is a BatchNormLayer
        if (!(targetLayer instanceof BatchNormLayer target)) {
            throw new IllegalArgumentException("Target layer must be a BatchNormLayer");
        }

        // Copy fields from the current BatchNormLayer to the target BatchNormLayer
        System.arraycopy(this.gamma, 0, target.gamma, 0, this.gamma.length);
        System.arraycopy(this.beta, 0, target.beta, 0, this.beta.length);
        System.arraycopy(this.runningMean, 0, target.runningMean, 0, this.runningMean.length);
        System.arraycopy(this.runningVar, 0, target.runningVar, 0, this.runningVar.length);
        System.arraycopy(this.dGamma, 0, target.dGamma, 0, this.dGamma.length);
        System.arraycopy(this.dBeta, 0, target.dBeta, 0, this.dBeta.length);

        if (!ignorePrimitives) {
            // Copy fields from the Layer superclass (manually)
            target.inputSize = this.inputSize;
            target.outputSize = this.outputSize;
            target.alpha = this.alpha;
        }

        // Copy other fields specific to BatchNormLayer
        target.depth = this.depth;
        target.height = this.height;
        target.width = this.width;
        target.epsilon = this.epsilon;
        target.momentum = this.momentum;
    }

    @Override
    public Layer copy() {
        BatchNormLayer copy = new BatchNormLayer(depth, height, width);
        copyTo(copy, false);
        return copy;
    }

    @Override
    public String toString() {
        return "BatchNormLayer: in:" + inputSize + " out:" + outputSize;
    }

    public int getDepth() {
        return depth;
    }

    public double[] getGradientGamma() {
        return dGamma;
    }

    public double[] getGradientBeta() {
        return dBeta;
    }

    @Override
    public void dumpInfo() {}
}
