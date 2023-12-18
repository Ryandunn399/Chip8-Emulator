package me.ryan;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;

/**
 * Main class of the project that's primary responsibility
 * as of right now is to render the screen.
 */
public class Chip8 extends Application {

    private Timeline timeline;
    private Screen screen;
    private Memory memory;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Chip-8 Emulator");
        stage.setResizable(false);

        screen = new Screen();
        Group root = new Group(screen);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        KeyFrame kf = new KeyFrame(Duration.millis(3), e -> {
            memory.fetch();
            memory.execute();

            if (memory.isUpdateScreen()) {
                screen.draw();
                memory.setUpdateScreen(false);
            }
        });

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.setAutoReverse(true);
        timeline.getKeyFrames().add(kf);

        loadProgram("roms/ibm.ch8");

        stage.show();
    }

    private void loadProgram(String path) {
        timeline.stop();
        screen.clear();

        memory = new Memory(screen);

        try {
            File file = new File(path);

            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

            byte[] bytes = new byte[(int) file.length()];
            in.read(bytes);

            memory.loadProgram(bytes);

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        timeline.play();
    }

    public static void main(String[] args) {
        launch();
    }
}

/*@Override
    public void start(Stage stage) {
        stage.setTitle("Chip-8 Emulator");
        stage.setResizable(false);

        Group root = new Group();

        // Placeholder
        Rectangle rect = new Rectangle();
        rect.setFill(Color.BLUE);
        rect.setHeight(100);
        rect.setWidth(100);
        rect.relocate(0, 0);
        root.getChildren().add(rect);
        // Endof placeholder

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }*/
