package Structures;

import Tools.math;

import java.io.Serializable;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * Represents a matrix of doubles with various operations.
 * <p>
 * This class provides methods for matrix manipulation, including addition,
 * subtraction, multiplication, division, and other element-wise operations.
 * It also supports parallel computation for certain operations.
 * </p>
 */
public class MatrixDouble implements Serializable {
    private static final int TILE_SIZE = 32;
    private static final int UNROLL_FACTOR = 4;
    private static final int PARALLELISM_THRESHOLD = 1024;
    private static final ForkJoinPool POOL = ForkJoinPool.commonPool();

    private double[][] data;
    int rows, cols;

    public MatrixDouble(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive.");
        }
        this.rows = rows;
        this.cols = cols;

        data = new double[rows][cols];
    }

    public MatrixDouble(double[][] data) {
        if (data == null || data.length == 0 || data[0].length == 0) {
            throw new IllegalArgumentException("Data array must be non-empty.");
        }
        this.data = data;
        this.rows = data.length;
        this.cols = data[0].length;
    }

    public MatrixDouble(double[] data, int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive.");
        }
        if (data.length != rows * cols) {
            throw new IllegalArgumentException("Data length does not match the specified dimensions");
        }
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data, i * cols, this.data[i], 0, cols);
        }
    }

    public static MatrixDouble elementwiseSquare(MatrixDouble matrix) {
        MatrixDouble result = new MatrixDouble(matrix.rows, matrix.cols);
        for (int i = 0; i < matrix.rows; i++) {
            for (int j = 0; j < matrix.cols; j++) {
                result.data[i][j] = matrix.data[i][j] * matrix.data[i][j];
            }
        }
        return result;
    }

    public static MatrixDouble elementwiseSquareRoot(MatrixDouble matrix) {
        MatrixDouble result = new MatrixDouble(matrix.rows, matrix.cols);
        for (int i = 0; i < matrix.rows; i++) {
            for (int j = 0; j < matrix.cols; j++) {
                result.data[i][j] = Math.sqrt(matrix.data[i][j]);
            }
        }
        return result;
    }

    public static MatrixDouble elementWiseDivide(MatrixDouble matrix, MatrixDouble divisor) {
        if (matrix.rows != divisor.rows || matrix.cols != divisor.cols) {
            throw new IllegalArgumentException("Matrices must have the same dimensions for element-wise division.");
        }

        MatrixDouble result = new MatrixDouble(matrix.rows, matrix.cols);
        for (int i = 0; i < matrix.rows; i++) {
            for (int j = 0; j < matrix.cols; j++) {
                if (divisor.data[i][j] == 0) {
                    throw new IllegalArgumentException("Division by zero encountered in matrix.");
                }
                result.data[i][j] = matrix.data[i][j] / divisor.data[i][j];
            }
        }

        return result;
    }

    public static MatrixDouble subtract(MatrixDouble inputMatrix, double mean) {
        MatrixDouble result = new MatrixDouble(inputMatrix.rows, inputMatrix.cols);
        for (int i = 0; i < inputMatrix.rows; i++) {
            for (int j = 0; j < inputMatrix.cols; j++) {
                result.data[i][j] = inputMatrix.data[i][j] - mean;
            }
        }
        return result;
    }

    public void fill(double value) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = value;
            }
        }
    }

    public void randomize() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = Math.random() * 2 - 1;
            }
        }
    }
    public void randomize(double min, double max) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = math.randomDouble(min, max);
            }
        }
    }

    public void add(double n) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] += n;
            }
        }
    }

    public static MatrixDouble add(MatrixDouble matrix, double v) {
        MatrixDouble result = new MatrixDouble(matrix.getRows(), matrix.getCols());
        for (int r = 0; r < matrix.getRows(); r++) {
            for (int c = 0; c < matrix.getCols(); c++) {
                result.set(c, r, matrix.get(c, r) + v);
            }
        }
        return result;
    }

    public void add(int row, int column, double value) {
        data[row][column] += value;
    }

    public void add(MatrixDouble m) {
        if (rows != m.rows || cols != m.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] += m.data[i][j];
            }
        }
    }

    public void subtract(double n) {
        add(-n);
    }

    public void subtract(MatrixDouble m) {
        if (rows != m.rows || cols != m.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] -= m.data[i][j];
            }
        }
    }

    public void multiply(double n) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] *= n;
            }
        }
    }

    public double sumOfSquares() {
        double sum = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sum += data[i][j] * data[i][j];
            }
        }
        return sum;
    }

    public void divide(double scalar) {
        if (scalar == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] /= scalar;
            }
        }
    }

    public static MatrixDouble divide(MatrixDouble matrix, double scalar) {
        MatrixDouble result = matrix.copy();
        result.divide(scalar);
        return result;
    }

    public MatrixDouble transpose() {
        return transpose(this.copy());
    }

    public String dims() {
        return "[r:"+rows+", c:"+cols+"]";
    }

    public MatrixDouble copy() {
        double[][] newData = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data[i], 0, newData[i], 0, cols);
        }
        return new MatrixDouble(newData);
    }

    public static void copy(MatrixDouble source, MatrixDouble target) {
        if (source.rows != target.rows || source.cols != target.cols) {
            throw new IllegalArgumentException("Source and target matrices must have the same dimensions.");
        }
        for (int i = 0; i < source.rows; i++) {
            System.arraycopy(source.data[i], 0, target.data[i], 0, source.cols);
        }
    }

    public MatrixDouble clip(double min, double max) {
        MatrixDouble result = new MatrixDouble(this.rows, this.cols);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.cols; c++) {
                result.set(c, r, Math.max(min, Math.min(max, this.get(c, r))));
            }
        }
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        final int dp = 3;
        int multiplier = (int) Math.pow(10, dp);
        for (int i = 0; i < rows; i++) {
            sb.append("\n[");
            for (int j = 0; j < cols; j++) {
                double roundedVal = (double) Math.round(data[i][j] * multiplier) /multiplier;
                sb.append(roundedVal);
                if (j < cols - 1) {
                    sb.append(",\t");
                }
            }
            sb.append("]");
            if (i < rows - 1) {
                sb.append(",");
            }
        }
        sb.append("\n]");
        return sb.toString();
    }

    public double get(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            throw new IndexOutOfBoundsException(
                    String.format("Attempted to access element at (%d, %d) in a [%d x %d] matrix.", x, y, rows, cols));
        }
        return data[y][x];
    }

    public void set(int x, int y, double value) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            throw new IndexOutOfBoundsException(
                    String.format("Attempted to set element at (%d, %d) in a [%d x %d] matrix.", x, y, rows, cols));
        }
        data[y][x] = value;
    }

    /*
    -----------------------------------------------------------------------------

    STATIC METHODS

    -----------------------------------------------------------------------------
     */

    public static MatrixDouble add(MatrixDouble a, MatrixDouble b) {
        MatrixDouble res = a.copy();
        if (a.rows != b.rows || a.cols != b.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }

        for (int i = 0; i < res.rows; i++) {
            for (int j = 0; j < res.cols; j++) {
                res.data[i][j] = a.data[i][j] + b.data[i][j];
            }
        }

        return res;
    }

    public static MatrixDouble subtract(MatrixDouble a, MatrixDouble b) {
        if (a.rows != b.rows || a.cols != b.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }
        MatrixDouble res = new MatrixDouble(a.rows, a.cols);
        for (int i = 0; i < res.rows; i++) {
            for (int j = 0; j < res.cols; j++) {
                res.data[i][j] = a.data[i][j] - b.data[i][j];
            }
        }
        return res;
    }

    public static MatrixDouble multiply(MatrixDouble matrix, double value) {
        MatrixDouble res = matrix.copy();
        res.multiply(value);
        return res;
    }

    public static MatrixDouble transpose(MatrixDouble matrix) {
        int rows = matrix.data.length;
        int cols = matrix.data[0].length;
        MatrixDouble transposed = new MatrixDouble(cols, rows);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed.data[j][i] = matrix.data[i][j];
            }
        }

        return transposed;
    }

    public static MatrixDouble getIdentityMatrix(int width) {
        double[][] data = new double[width][width];
        for (int i = 0; i < width; i++) {
            data[i][i] = 1;
        }
        return new MatrixDouble(data);
    }
    public static MatrixDouble getIdentityMatrix(MatrixDouble m) {
        assert (m.rows == m.cols); // Make sure matrix m is a square matrix
        return getIdentityMatrix(m.cols);
    }

    //                                          1           2
    public static MatrixDouble getNumberedMatrix(int width, int height) {
        MatrixDouble m = new MatrixDouble(height, width);
        //                      1       2

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                m.data[j][i] = width*j + i + 1;
            }
        }

        return m;
    }

    /*
    -----------------------------------------------------------------------------

    MULTIPLICATION

    -----------------------------------------------------------------------------
    */

    public static MatrixDouble elementWiseMultiply(MatrixDouble A, MatrixDouble B) {
        if (A.rows != B.rows || A.cols != B.cols) {
            throw new IllegalArgumentException("Matrices must have the same dimensions for element-wise multiplication. (A:"+A.dims()+" != B:"+B.dims()+")");
        }

        MatrixDouble result = new MatrixDouble(A.rows, A.cols);

        IntStream.range(0, A.rows).parallel().forEach(i -> {
            for (int j = 0; j < A.cols; j++) {
                result.data[i][j] = A.data[i][j] * B.data[i][j];
            }
        });

        return result;
    }

    public static MatrixDouble multiply(MatrixDouble A, MatrixDouble B) {
        if (A.cols != B.rows) {
            System.out.println(B);
            throw new IllegalArgumentException("A's columns must match B's rows ("+A.cols+"!="+B.rows+") - A.dims="+A.dims()+", B.dims="+B.dims());
        }

        AtomicReference<double[][]> C = new AtomicReference<>(new double[A.rows][B.cols]);
        MatrixDouble BT = transpose(B);

        POOL.invoke(new MultiplyTask(A, BT, C, 0, A.rows, 0, B.cols, 0, A.cols));

        return new MatrixDouble(C.get());
    }

    public void multiply(MatrixDouble B) {
        MatrixDouble res = multiply(this, B);
        this.rows = res.rows;
        this.cols = res.cols;
        this.data = res.data;
    }

    public int getHeight() {
        return rows;
    }
    public int getRows() {
        return rows;
    }

    public int getWidth() {
        return cols;
    }
    public int getCols() {
        return cols;
    }

    public MatrixDouble toRowMatrix() {
        MatrixDouble result = new MatrixDouble(1, rows*cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(r*cols+c, 0, get(c, r));
            }
        }
        return result;
    }

    public MatrixDouble toColumnMatrix() {
        MatrixDouble result = new MatrixDouble(rows*cols, 1);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.set(0, r*cols+c, get(c, r));
            }
        }
        return result;
    }

    public double getSum() {
        double sum = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sum += data[i][j];
            }
        }
        return sum;
    }

    public double getMeanAverage() {
        return getSum() / (rows * cols);
    }

    public double getVariance(double mean) {
        double sum = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double diff = data[i][j] - mean;
                sum += diff * diff;
            }
        }
        return sum / (rows * cols);
    }

    private static class MultiplyTask extends RecursiveAction {
        private final MatrixDouble A, BT;
        private final AtomicReference<double[][]> C;
        private final int rowStart, rowEnd, colStart, colEnd, depthStart, depthEnd;

        MultiplyTask(MatrixDouble A, MatrixDouble BT, AtomicReference<double[][]> C,
                     int rowStart, int rowEnd,
                     int colStart, int colEnd,
                     int depthStart, int depthEnd) {
            this.A = A;
            this.BT = BT;
            this.C = C;
            this.rowStart = rowStart;
            this.rowEnd = rowEnd;
            this.colStart = colStart;
            this.colEnd = colEnd;
            this.depthStart = depthStart;
            this.depthEnd = depthEnd;
        }

        @Override
        protected void compute() {
            int rowSize = rowEnd - rowStart;
            int colSize = colEnd - colStart;
            int depthSize = depthEnd - depthStart;

            if (rowSize * colSize * depthSize <= PARALLELISM_THRESHOLD) {
                multiplySequential();
                return;
            }

            if (rowSize >= colSize && rowSize >= depthSize) {
                int mid = rowStart + rowSize / 2;
                invokeAll(
                        new MultiplyTask(A, BT, C, rowStart, mid, colStart, colEnd, depthStart, depthEnd),
                        new MultiplyTask(A, BT, C, mid, rowEnd, colStart, colEnd, depthStart, depthEnd)
                );
            } else if (colSize >= depthSize) {
                int mid = colStart + colSize / 2;
                invokeAll(
                        new MultiplyTask(A, BT, C, rowStart, rowEnd, colStart, mid, depthStart, depthEnd),
                        new MultiplyTask(A, BT, C, rowStart, rowEnd, mid, colEnd, depthStart, depthEnd)
                );
            } else {
                int mid = depthStart + depthSize / 2;
                invokeAll(
                        new MultiplyTask(A, BT, C, rowStart, rowEnd, colStart, colEnd, depthStart, mid),
                        new MultiplyTask(A, BT, C, rowStart, rowEnd, colStart, colEnd, mid, depthEnd)
                );
            }
        }

        private void multiplySequential() {
            double[][] localC = new double[rowEnd - rowStart][colEnd - colStart];
            for (int i0 = rowStart; i0 < rowEnd; i0 += TILE_SIZE) {
                for (int j0 = colStart; j0 < colEnd; j0 += TILE_SIZE) {
                    for (int k0 = depthStart; k0 < depthEnd; k0 += TILE_SIZE) {
                        multiplyTile(i0, j0, k0, localC);
                    }
                }
            }
            mergeResult(localC);
        }

        private void multiplyTile(int i0, int j0, int k0, double[][] localC) {
            int iMax = Math.min(i0 + TILE_SIZE, rowEnd);
            int jMax = Math.min(j0 + TILE_SIZE, colEnd);
            int kMax = Math.min(k0 + TILE_SIZE, depthEnd);

            for (int i = i0; i < iMax; i++) {
                for (int j = j0; j < jMax; j += UNROLL_FACTOR) {
                    double sum0 = 0, sum1 = 0, sum2 = 0, sum3 = 0;

                    for (int k = k0; k < kMax; k++) {
                        double aik = A.data[i][k];
                        double product = aik * BT.data[j][k];
                        sum0 += product;

                        if (j + 1 < jMax) sum1 += aik * BT.data[j + 1][k];
                        if (j + 2 < jMax) sum2 += aik * BT.data[j + 2][k];
                        if (j + 3 < jMax) sum3 += aik * BT.data[j + 3][k];
                    }

                    localC[i - rowStart][j - colStart] += sum0;
                    if (j + 1 < jMax) localC[i - rowStart][j + 1 - colStart] += sum1;
                    if (j + 2 < jMax) localC[i - rowStart][j + 2 - colStart] += sum2;
                    if (j + 3 < jMax) localC[i - rowStart][j + 3 - colStart] += sum3;
                }
            }
        }

        private void mergeResult(double[][] localC) {
            double[][] globalC = C.get();
            synchronized (C) {
                for (int i = 0; i < localC.length; i++) {
                    for (int j = 0; j < localC[0].length; j++) {
                        if (Double.isNaN(localC[i][j]) || Double.isInfinite(localC[i][j])) {
                            throw new IllegalArgumentException("NaN or Infinity encountered in matrix multiplication");

                        }
                        globalC[i + rowStart][j + colStart] += localC[i][j];
                    }
                }
            }
        }
    }
}
