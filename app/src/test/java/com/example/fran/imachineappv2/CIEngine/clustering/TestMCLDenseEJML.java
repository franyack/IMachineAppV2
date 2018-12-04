package com.example.fran.imachineappv2.CIEngine.clustering;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

// See https://github.com/ThomasJaspers/java-junit-sample

public class TestMCLDenseEJML {
    private MCLDenseEJML mcl;
    private DMatrixRMaj mat;

    private static final int nDim = 100;
    private static final int maxIt = 100;
    private static final int expPow = 2;
    private static final int infPow = 3;
    private static final double epsConvergence = 1e-3;
    private static final double threshPrune = 0.01;
    private static final int seed = 123;



    @Before
    public void setUp(){
        mcl = new MCLDenseEJML(maxIt, expPow, infPow, epsConvergence, threshPrune);
        mat = mcl.generateRandomAffinityMatrix(nDim, seed);
    }

    @Test
    public void runMCL(){
        setUp();

        // Run MCL
        DMatrixRMaj result = mcl.run(mat);

        // Assert some properties on resulting matrix
        Assert.assertEquals(result.numCols * result.numRows, nDim);
        Assert.assertTrue(CommonOps_DDRM.elementMax(result) > 0.0);
        Assert.assertTrue(CommonOps_DDRM.elementMin(result) == 0.0);
    }
}
