package me.ryan;

import java.util.Stack;

/**
 * Representation of Chip-8 memory.
 */
public class Memory {

    /**
     * Chip-8 had a memory size of 4KB represented here.
     */
    public static final int MEM_SIZE = 4096;

    /**
     * This array will act as our runtime memory.
     */
    private final int[] memory;

    /**
     * Chip-8 stack.  The "stack pointer" is already kept updated in the
     * java data structure.
     */
    private final Stack<Integer> stack;

    private final Screen screen;

    /**
     * Register for pointing at the current instruction in memory.
     */
    private int pc;

    /**
     * Index register to point at locations in memory.
     */
    private int I;

    /**
     * We will store the current operation code here.
     */
    private int opcode;

    /**
     * The array representing the 16 registers.
     */
    private final int[] V;

    /**
     * We will use this boolean to allow the renderer to determine whether or not
     * the screen needs to be drawn with new values.
     */
    private boolean updateScreen;


    private int delayTimer;

    private int cycleCount;

    /**
     * Constructor for our memory class.
     */
    public Memory(Screen screen) {
        this.memory = new int[MEM_SIZE];
        this.V = new int[NUM_REGISTERS];
        this.screen = screen;
        this.updateScreen = false;
        this.delayTimer = 0;
        this.cycleCount = 0;

        stack = new Stack<>();
        pc = MEM_START;
    }

    public void loadProgram(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            memory[i + MEM_START] = (b[i] & BYTE_MASK);
        }
    }

    /**
     * Testing function to delete.
     */
    public int getByte(int index) {
        return memory[index];
    }

    /**
     * Every cell of the memory array represents one of memory, so
     * we need to read two bytes at a time.
     *
     * @return the instruction at the program counter.
     */
    public int fetch() {
        opcode = (memory[pc] << 8 | memory[pc + 1]);
        pc += 2;
        return opcode;
    }

    /**
     * Will decode the current instruction given by opcode.
     */
    public int execute() {
        // Separate switch statement for exact instructions that don't follow
        // typical standard of first nibble being the instr.
        if (opcode == CLEAR_SCREEN) {
            screen.clear();
            //System.out.printf("(%d): CLS      #(%x)\n", cycles, opcode);
            return CLEAR_SCREEN;
        }

        if (opcode == SUBROUTINE_RESTORE) {
            pc = stack.pop();
            return SUBROUTINE_RESTORE;
        }

        return switch (opcode & INSTR_MASK) {
            case JUMP -> {
                pc = (opcode & NNN_MASK);
                //System.out.printf("(%d): JP %x    #(%x)\n", cycles, pc, opcode);
                yield JUMP;
            }

            case SET_REGISTER -> {
                int register = ((opcode & XNXX_MASK) >>> 8);
                int value = ((opcode & NN_MASK));
                V[register] = value;
                //System.out.printf("(%d): LD V%d, %d   #(%x)\n", cycles, register, value, opcode);
                yield SET_REGISTER;
            }

            case ADD_VALUE -> {
                int register = ((opcode & XNXX_MASK) >>> 8);
                int value = ((opcode & NN_MASK));
                int result = V[register] + value;

                // Overflow handler.
                if (result >= 256) {
                    V[register] = result - 256;
                } else {
                    V[register] = result;
                }

                yield ADD_VALUE;
            }

            case SET_INDEX -> {
                I = (opcode & NNN_MASK);
                //System.out.printf("(%d): LD I, L%x   #(%x)\n", cycles, I, opcode);
                yield SET_INDEX;
            }

            case DISPLAY -> {
                int vx = V[((opcode & XNXX_MASK) >> 8)];
                int vy = V[((opcode & XXNX_MASK) >> 4)];
                int height = (opcode & XXXN_MASK);
                V[0xF] = 0;

                for (int yVal = 0; yVal < height; yVal++) {
                    int spriteData = memory[I + yVal];

                    for (int xVal = 0; xVal < 8; xVal++) {
                        if ((spriteData & (SPRITE_MASK >>> xVal)) != 0) {

                            int xCoord = vx + xVal;
                            int yCoord = vy + yVal;

                            if (screen.getPixel(xCoord, yCoord) == 1) {
                                V[0xF] = 1;
                            }

                            screen.updatePixel(xCoord, yCoord);
                        }
                    }
                }

                updateScreen = true;
                yield DISPLAY;
            }

            case SUBROUTINE_JUMP -> {
                int addr = (opcode & NNN_MASK);
                stack.push(pc);
                pc = addr;
                yield SUBROUTINE_JUMP;
            }

            case SKIP_IF_VX -> {
                int vx = V[((opcode & XNXX_MASK) >> 8)];
                int value = (opcode & NN_MASK);

                if (value == vx) {
                    pc += 2;
                }

                yield SKIP_IF_VX;
            }

            case SKIP_IF_NOT_VX -> {
                int vx = V[((opcode & XNXX_MASK) >> 8)];
                int value = (opcode & NN_MASK);

                if (value != vx) {
                    pc += 2;
                }

                yield SKIP_IF_NOT_VX;
            }

            case SKIP_IF_VX_VY -> {
                int vx = V[((opcode & XNXX_MASK) >> 8)];
                int vy = V[((opcode & XXNX_MASK) >> 4)];

                if (vx == vy) {
                    opcode += 2;
                }

                yield SKIP_IF_VX_VY;
            }

            case SKIP_IF_NOT_VX_VY -> {
                int vx = V[((opcode & XNXX_MASK) >> 8)];
                int vy = V[((opcode & XXNX_MASK) >> 4)];

                if (vx != vy) {
                    opcode += 2;
                }

                yield SKIP_IF_NOT_VX_VY;
            }

            case LOGICAL_INSTR -> {
                int x = ((opcode & XNXX_MASK) >> 8);
                int y = ((opcode & XXNX_MASK) >> 4);

                // SET VX to VY
                if ((opcode & 0xF) == 0) {
                    V[x] = V[y];
                    yield LOGICAL_INSTR;
                }

                // Binary OR
                if ((opcode & 0xF) == 1) {
                    V[x] |= V[y];
                    yield LOGICAL_INSTR;
                }

                // Binary and
                if ((opcode & 0xF) == 2) {
                    V[x] &= V[y];
                    yield LOGICAL_INSTR;
                }

                // Binary XOR
                if ((opcode & 0xF) == 3) {
                    V[x] ^= V[y];
                    yield LOGICAL_INSTR;
                }

                // Add with carry flag
                if ((opcode & 0xF) == 4) {
                    int value = V[x] + V[y];

                    // Overflow with carry
                    V[0xF] = value > 0xFF ? 1 : 0;
                    V[x] = (value & 0xFF);

                    yield LOGICAL_INSTR;
                }

                // Subtract VX - VY
                if ((opcode & 0xF) == 5) {
                    if (V[x] > V[y]) {
                        V[0xF] = 1;
                    } else {
                        V[0xF] = 0;
                    }

                    V[x] = ((V[x] - V[y]) & 0xFF);
                    yield LOGICAL_INSTR;
                }

                // Shift
                if ((opcode & 0xF) == 6) {
                    V[0xF] = V[x] & 0x1;
                    V[x] = V[x] >>> 1;
                    yield LOGICAL_INSTR;
                }

                // Subtract VY - VX
                if ((opcode & 0xF) == 7) {
                    if (V[y] > V[x]) {
                        V[0xF] = 1;
                    } else {
                        V[0xF] = 0;
                    }

                    V[x] = ((V[y] - V[x]) & 0xFF);
                    yield LOGICAL_INSTR;
                }

                if ((opcode & 0xF) == 0xE) {
                    V[0xF] = (V[x] & 0x80) >>> 7;
                    V[x] = ((V[x] << 1) & 0xFF);
                    yield LOGICAL_INSTR;
                }

                yield LOGICAL_INSTR;
            }

            default -> 0;
        };
    }

    /**
     *
     * @return the update screen flag.
     */
    public boolean isUpdateScreen() {
        return this.updateScreen;
    }

    /**
     *
     * @return the value of the delay timer.
     */
    public int getDelayTimer() {
        return delayTimer;
    }

    /**
     * Update the delay timer if it is greater than zero.
     */
    public void updateDelayTimer() {
        cycleCount += 1;
        if (cycleCount > 16) {
            cycleCount = 0;

            if (this.delayTimer > 0) {
                this.delayTimer -= 1;
            }
        }
    }

    /**
     *
     * @param updateScreen new value for our update screen flag.
     */
    public void setUpdateScreen(boolean updateScreen) {
        this.updateScreen = updateScreen;
    }

    //////////////////////////////////////////////
    //                CONSTANTS                 //
    //////////////////////////////////////////////

    public static final int CLEAR_SCREEN = 0xE0;
    public static final int SUBROUTINE_RESTORE = 0xEE;
    public static final int BYTE_MASK = 0xFF;
    public static final int MEM_START = 0x200;
    public static final int JUMP = 0x1000;
    public static final int SUBROUTINE_JUMP = 0x2000;
    public static final int SKIP_IF_VX = 0x3000;
    public static final int SKIP_IF_NOT_VX = 0x4000;
    public static final int SKIP_IF_VX_VY = 0x5000;
    public static final int SET_REGISTER = 0x6000;
    public static final int ADD_VALUE = 0x7000;
    public static final int LOGICAL_INSTR = 0x8000;
    public static final int SKIP_IF_NOT_VX_VY = 0x9000;
    public static final int SET_INDEX = 0xA000;
    public static final int DISPLAY = 0xD000;
    public static final int FUNCTIONS = 0xF000;


    public static final int INSTR_MASK = 0xF000;
    public static final int NNN_MASK = 0x0FFF;
    public static final int NN_MASK = 0x00FF;
    public static final int XNXX_MASK = 0x0F00;
    public static final int XXNX_MASK = 0x00F0;
    public static final int XXXN_MASK = 0x000F;
    public static final int SPRITE_MASK = 0x80;

    public static final int NUM_REGISTERS = 16;

    public static final int MAX_CYCLES = 39;
}
