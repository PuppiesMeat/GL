package com.hs.firstopengl

import android.content.Context
import androidx.annotation.NonNull
import androidx.annotation.RawRes
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import android.opengl.GLES20.*
import android.util.Log

object OpenGLUtil {

    val TAG = OpenGLUtil::class.simpleName

    public fun readRawShader(@NonNull context: Context, @RawRes rawResId: Int): String? {
        try {
            val bufferedReader =
                BufferedReader(InputStreamReader(context.resources.openRawResource(rawResId)))
            val sb = StringBuilder()
            do {
                var readLine = bufferedReader.readLine()
                if (null != readLine) {
                    sb.append(readLine).append("\n")
                } else {
                    break
                }
            } while (true)
            return sb.toString()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return null
    }

    public fun compileVertexShader(@NonNull vertexCode: String): Int {
        return compileShader(GL_VERTEX_SHADER, vertexCode)
    }

    public fun compileFragShader(@NonNull fragCode: String): Int {
        return compileShader(GL_FRAGMENT_SHADER, fragCode)
    }

    private fun compileShader(type: Int, shaderCode: String): Int {
        //创建一个着色器对象，返回的Int整型值就是OpenGL对象的引用，之后需要引用这个对象，都需要将这个值传给OpenGL
        val shaderObjectId = glCreateShader(type)
        if (0 == shaderObjectId) {
            Log.e(TAG, "compileShader failed! type: $type")
            return 0
        }

        //上传OpenGL代码
        glShaderSource(shaderObjectId, shaderCode)

        //通知OpenGL编译之前上传的着色器代码
        glCompileShader(shaderObjectId)

        //检查是否编译成功
        val compileStatus = intArrayOf(0)
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0)
        Log.d(
            TAG,
            "compile status ${glGetShaderInfoLog(shaderObjectId)} \n shaderCode: $shaderCode"
        )

        //验证编译状态并返回着色器对象ID
        if (0 == compileStatus[0]) {//编译失败 删除着色器
            glDeleteShader(shaderObjectId)
        }
        return shaderObjectId
    }

    public fun linkProgram(vertexObjectId: Int, fragObjectId: Int): Int {
        //创建OpenGL程序 返回的是OpenGL程序实例引用 创建失败则返回0
        var programObjectId = glCreateProgram()
        if (0 == programObjectId) {
            Log.e(TAG, "createProgram failed")
            return 0
        }

        //附上着色器
        glAttachShader(programObjectId, vertexObjectId)
        glAttachShader(programObjectId, fragObjectId)

        //链接程序 把这些着色器联合起来形成pipeline
        glLinkProgram(programObjectId)

        //检查链接是否失败
        val linkStatus = intArrayOf(0)
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0)
        if (0 == linkStatus[0]) {
            Log.e(
                TAG,
                "link program failed, error message: ${glGetProgramInfoLog(programObjectId)}"
            )
            glDeleteProgram(programObjectId)
            return 0
        }

        return programObjectId
    }


    public fun validateProgram(programObjectId: Int): Boolean {
        //我们调用glValidateProgram来验证程序 注意我们只有在开发和调试中去验证它。
        glValidateProgram(programObjectId)

        val validateStatus = intArrayOf(0)
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0)
        Log.d(TAG, "validateProgram: ${glGetProgramInfoLog(programObjectId)}")
        return 0 != validateStatus[0]
    }

}