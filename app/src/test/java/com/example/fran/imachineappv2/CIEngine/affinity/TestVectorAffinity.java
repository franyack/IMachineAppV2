package com.example.fran.imachineappv2.CIEngine.affinity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestVectorAffinity {
    private List<float[]> vector;
    private VectorAffinity vectorAffinity;

    private static final int N = 10;
    private static final int D = 5;
    private static final float THRESHOLD = 0.05f;


    private float[] getVector(int d, float offset){
        float[] v = new float[d];

        for (int i = 0; i < d; i++) {
            v[i] = ((float) i) / d + offset;
        }

        return v;
    }

    @Before
    public void setUp(){
        // Set up vector to be used
        vector = new ArrayList<>();

        float offset = 0.f;

        for (int i = 0; i < N; i++) {
            vector.add(getVector(D, offset));
            offset ++;
        }

        // Set up affinity class
        vectorAffinity = new VectorAffinity(THRESHOLD);
    }

    @Test
    public void getAffinityMatrix(){
        setUp();

        double[][] result = vectorAffinity.getAffinityMatrix(vector);

        for (int i = 0; i < N; i++) {
            // Assert all 1s in diagonal
            Assert.assertEquals(result[i][i], 1.0, 1e-3);

            // Assert symmetry -> (i,j) == (j,i)
            for (int j = 0; j < i; j++) {
                Assert.assertEquals(result[i][j], result[j][i], 1e-3);
            }
        }

    }
}
