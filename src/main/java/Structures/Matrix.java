package Structures;

import java.io.Serializable;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

public class Matrix implements Serializable {
    private static final int TILE_SIZE = 32;
    private static final int UNROLL_FACTOR = 4;
    private static final int PARALLELISM_THRESHOLD = 1024;
    private static final ForkJoinPool POOL = ForkJoinPool.commonPool();

    private float[][] data;
    int rows, cols;

    public Matrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        data = new float[rows][cols];
    }

    public Matrix(float[][] data) {
        this.data = data;
        this.rows = data.length;
        this.cols = data[0].length;
    }

    public void fill(float value) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = value;
            }
        }
    }

    public void randomize() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = (float) (Math.random() * 2 - 1);
            }
        }
    }

    public void add(float n) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] += n;
            }
        }
    }

    public void add(Matrix m) {
        if (rows != m.rows || cols != m.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] += m.data[i][j];
            }
        }
    }

    public void subtract(float n) {
        add(-n);
    }

    public void subtract(Matrix m) {
        if (rows != m.rows || cols != m.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] -= m.data[i][j];
            }
        }
    }

    public void multiply(float n) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] *= n;
            }
        }
    }

    public Matrix transpose() {
        float[][] newData = new float[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                newData[j][i] = data[i][j];
            }
        }
        data = newData;
        int temp = rows;
        rows = cols;
        cols = temp;
        return this;
    }

    private String dims() {
        return "[r:"+rows+", c:"+cols+"]";
    }

    public Matrix copy() {
        return new Matrix(data);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        final int dp = 3;
        int multiplier = (int) Math.pow(10, dp);
        for (int i = 0; i < rows; i++) {
            sb.append("\n[");
            for (int j = 0; j < cols; j++) {
                float roundedVal = (float) Math.round(data[i][j]*multiplier)/multiplier;
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

    public float get(int x, int y) {
        return data[y][x];
    }

    public void set(int x, int y, float value) {
        data[y][x] = value;
    }

    /*
    -----------------------------------------------------------------------------

    STATIC METHODS

    -----------------------------------------------------------------------------
     */

    public static Matrix add(Matrix a, Matrix b) {
        Matrix res = a.copy();
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

    public static Matrix subtract(Matrix a, Matrix b) {
        Matrix res = a.copy();
        if (a.rows != b.rows || a.cols != b.cols) {
            throw new IllegalArgumentException("The matrices must have the same dimensions.");
        }

        for (int i = 0; i < res.rows; i++) {
            for (int j = 0; j < res.cols; j++) {
                res.data[i][j] = a.data[i][j] - b.data[i][j];
            }
        }

        return res;
    }

    public static Matrix multiply(Matrix multiply, float learningRate) {
        Matrix res = multiply.copy();
        res.multiply(learningRate);
        return res;
    }

    private static Matrix transpose(Matrix matrix) {
        int rows = matrix.data.length;
        int cols = matrix.data[0].length;
        Matrix transposed = new Matrix(cols, rows);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                transposed.data[j][i] = matrix.data[i][j];
            }
        }

        return transposed;
    }

    public static Matrix getIdentityMatrix(int width) {
        float[][] data = new float[width][width];
        for (int i = 0; i < width; i++) {
            data[i][i] = 1;
        }
        return new Matrix(data);
    }
    public static Matrix getIdentityMatrix(Matrix m) {
        assert (m.rows == m.cols); // Make sure matrix m is a square matrix
        return getIdentityMatrix(m.cols);
    }

    //                                          1           2
    public static Matrix getNumberedMatrix(int width, int height) {
        Matrix m = new Matrix(height, width);
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

    public static Matrix multiply(Matrix A, Matrix B) {
        if (A.cols != B.rows) {
            throw new IllegalArgumentException("A's columns must match B's rows");
        }

        AtomicReference<float[][]> C = new AtomicReference<>(new float[A.rows][B.cols]);
        Matrix BT = transpose(B);

        POOL.invoke(new MultiplyTask(A, BT, C, 0, A.rows, 0, B.cols, 0, A.cols));

        return new Matrix(C.get());
    }

    public void multiply(Matrix B) {
        Matrix res = multiply(this, B);
        this.rows = res.rows;
        this.cols = res.cols;
        this.data = res.data;
    }

    public int getHeight() {
        return rows;
    }

    private static class MultiplyTask extends RecursiveAction {
        private final Matrix A, BT;
        private final AtomicReference<float[][]> C;
        private final int rowStart, rowEnd, colStart, colEnd, depthStart, depthEnd;

        MultiplyTask(Matrix A, Matrix BT, AtomicReference<float[][]> C,
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
            float[][] localC = new float[rowEnd - rowStart][colEnd - colStart];
            for (int i0 = rowStart; i0 < rowEnd; i0 += TILE_SIZE) {
                for (int j0 = colStart; j0 < colEnd; j0 += TILE_SIZE) {
                    for (int k0 = depthStart; k0 < depthEnd; k0 += TILE_SIZE) {
                        multiplyTile(i0, j0, k0, localC);
                    }
                }
            }
            mergeResult(localC);
        }

        private void multiplyTile(int i0, int j0, int k0, float[][] localC) {
            int iMax = Math.min(i0 + TILE_SIZE, rowEnd);
            int jMax = Math.min(j0 + TILE_SIZE, colEnd);
            int kMax = Math.min(k0 + TILE_SIZE, depthEnd);

            for (int i = i0; i < iMax; i++) {
                for (int j = j0; j < jMax; j += UNROLL_FACTOR) {
                    float sum0 = 0, sum1 = 0, sum2 = 0, sum3 = 0;

                    for (int k = k0; k < kMax; k++) {
                        float aik = A.data[i][k];
                        sum0 += aik * BT.data[j][k];
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

        private void mergeResult(float[][] localC) {
            float[][] globalC = C.get();
            synchronized (C) {
                for (int i = 0; i < localC.length; i++) {
                    for (int j = 0; j < localC[0].length; j++) {
                        globalC[i + rowStart][j + colStart] += localC[i][j];
                    }
                }
            }
        }
    }
}
