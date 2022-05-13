package com.hs.firstopengl.utils

object MatrixHelper {


    public final fun perspectiveM(
        m: FloatArray,
        yFovInDegrees: Float,
        aspect: Float,
        n: Float,
        f: Float
    ) {
        /**
         * 计算焦距
         */
        val angleInRadians: Float = (yFovInDegrees * Math.PI / 180.0).toFloat()

        /**
         * 我们使用Java的Math类计算那个正切函数，因为它需要弧度角，所以我们把视野从度转换为弧度。
         */
        val a: Float = (1.0 / Math.tan(angleInRadians / 2.0)).toFloat()


        /**
         * 输出矩阵
         */
        m[0] = a / aspect
        m[1] = 0f
        m[2] = 0f
        m[3] = 0f

        m[4] = 0f
        m[5] = a
        m[6] = 0f
        m[7] = 0f

        m[8] = 0f
        m[9] = 0f
        m[10] = -((f + n) / (f - n))
        m[11] = -1f

        m[12] = 0f
        m[13] = 0f
        m[14] = -((2f * f * n) / (f - n))
        m[15] = 0f
        /**
         * 这个数组至少需要16个元素，OpenGL把矩阵数据按照以列为主的顺序存储，这就意味着我们一次写一列数据，而不是一次写一行。
         */

    }
}