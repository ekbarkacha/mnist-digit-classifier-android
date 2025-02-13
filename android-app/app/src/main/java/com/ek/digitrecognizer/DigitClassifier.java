package com.ek.digitrecognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class DigitClassifier {
    private static final String MODEL_NAME = "mnist_digit_classifier.tflite";
    private Interpreter interpreter;
    private  Tensor inputTensor;
    private  org.tensorflow.lite.Tensor outputTensor;
    private  Size inputShape;
    private  int[] imagePixels;
    private ByteBuffer imageBuffer;
    private TensorBuffer outputBuffer;

    public DigitClassifier(Context context) throws IOException {
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_NAME);
        interpreter = new Interpreter(modelFile);
    }

    public Prediction classify(Bitmap bitmap) {
        long startTime = System.currentTimeMillis();  // Start time tracking

        inputTensor = interpreter.getInputTensor(0);
        outputTensor = interpreter.getOutputTensor(0);
        inputShape = new Size(inputTensor.shape()[2], inputTensor.shape()[1]);
        imagePixels = new int[inputShape.getHeight() * inputShape.getWidth()];
        imageBuffer = ByteBuffer.allocateDirect(4 * inputShape.getHeight() * inputShape.getWidth()).order(ByteOrder.nativeOrder());
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());

        imageBuffer.rewind();
        bitmap.getPixels(imagePixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < inputShape.getWidth() * inputShape.getHeight(); i++) {
            int pixel = imagePixels[i];
            imageBuffer.putFloat(convertPixel(pixel));
        }

        // Run inference
        interpreter.run(imageBuffer, outputBuffer.getBuffer().rewind());


        long endTime = System.currentTimeMillis();  // End time tracking
        long inferenceTime = endTime - startTime;  // Calculate time taken

        // Get prediction and confidence
        float[] probs = outputBuffer.getFloatArray();
        int predictedDigit = argMax(probs);
        float confidence = probs[predictedDigit] * 100;  // Convert to percentage


        return new Prediction(predictedDigit, confidence, inferenceTime);
    }

    private float convertPixel(int color) {
        return (255 - ((color >> 16 & 0xFF) * 0.299f
                + (color >> 8 & 0xFF) * 0.587f
                + (color & 0xFF) * 0.114f)) / 255.0f;
    }

    private int argMax(float[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }


    public void close() {
        interpreter.close();
    }

    public static class Prediction {
        public int digit;
        public float confidence;
        public long timeTaken;

        public Prediction(int digit, float confidence, long timeTaken) {
            this.digit = digit;
            this.confidence = confidence;
            this.timeTaken = timeTaken;
        }
    }
}
