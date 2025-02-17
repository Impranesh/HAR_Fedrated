package com.ascetx.har

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.widget.Toast
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ActivityClassifier(context: Context) {

    companion object {
        private const val MODEL_FILE = "model.tflite"
        private const val INPUT_SIZE = 300  // 1 * 100 * 3 (flattened for TFLite)
        private const val OUTPUT_SIZE = 7  // Number of output classes
    }

    private val tflite: Interpreter

    init {
        tflite = Interpreter(loadModelFile(context, MODEL_FILE), getInterpreterOptions())
        printInputFormat(context)
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFilename: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getInterpreterOptions(): Interpreter.Options {
        val options = Interpreter.Options()
        // Add Flex delegate
        val flexDelegate = FlexDelegate()
        options.addDelegate(flexDelegate)
        return options
    }

    fun predictProbabilities(data: FloatArray): FloatArray {
        val input = Array(1) { Array(100) { FloatArray(9) } }
        for (i in data.indices) {
            input[0][i / 9][i % 9] = data[i]
        }
        val output = Array(1) { FloatArray(OUTPUT_SIZE) }
        tflite.run(input, output)
        return output[0]
    }

    private fun printInputFormat(context: Context) {
        val inputTensor: Tensor = tflite.getInputTensor(0)
        val inputShape = inputTensor.shape()
        val inputDataType = inputTensor.dataType()

        // Create the message to display in the Toast
        val message = "Input Shape: ${inputShape.contentToString()}\nInput Data Type: $inputDataType"

        // Show the Toast message
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
