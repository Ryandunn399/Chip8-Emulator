package me.ryan;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Screen for our Chip-8 emulator that will handle the drawingo of pixels.
 *
 */
public class Screen extends Canvas {

    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 400;

    public static final int WIDTH = 64;
    public static final int HEIGHT = 32;
    public static final int SCALE = 12;

    private int[][] pixels;
    private final GraphicsContext gc;

    /**
     * Constructor for our screen.
     */
    public Screen() {
        super(WINDOW_WIDTH, WINDOW_HEIGHT);

        gc = this.getGraphicsContext2D();
        pixels = new int[WIDTH][HEIGHT];

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    /**
     * Will draw the pixels on the screen.
     */
    public void draw() {
        for (int x = 0; x < pixels.length; x++) {
            for (int y = 0; y < pixels[y].length; y++) {
                if (pixels[x][y] == 0) {
                    gc.setFill(Color.BLACK);
                } else {
                    gc.setFill(Color.WHITE);
                }

                gc.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
            }
        }
    }

    /**
     * Clear the screen and reset the pixels.
     */
    public void clear() {
        pixels = new int[WIDTH][HEIGHT];
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    /**
     * Will retrieve the pixel value at a given coordinate.
     *
     * @param x x-coordinate of our pixel.
     * @param y y-coordinate of our pixel.
     * @return the current value of our pixel (0 or 1)
     */
    public int getPixel(int x, int y) {
        return this.pixels[x][y];
    }

    /**
     * Updates a pixel by XORing it.
     *
     * @param x x-coordinate for the pixel.
     * @param y y-coordinate for the pixel.
     */
    public void updatePixel(int x, int y) {
        this.pixels[x][y] ^= 1;
    }
}
