package emu;

import java.io.FileNotFoundException;

import chip.Chip;

public class Main extends Thread {

    private Chip chip8;
    private ChipFrame frame;

    public Main() throws FileNotFoundException {
        chip8= new Chip();
        chip8.init();
        chip8.loadProgram("src\\invaders.c8");
        frame= new ChipFrame(chip8);
    }

    @Override
    public void run() {
        while (true) {
            chip8.setKeyBuffer(frame.getKeyBuffer());
            chip8.run();
            if (chip8.needsRedraw()) {
                frame.repaint();
                chip8.removeDrawFlag();
            }
            try {
                Thread.sleep(8);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        Main main= new Main();
        main.start();
    }

}
