package gitlet;

import java.io.File;
import java.util.Arrays;
// import gitlet.Repository;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Sergio W. Peterson
 */
public class Main {
    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        Repository gitlet = new Repository();
        switch (firstArg) {
            case "init":
                gitlet.init();
                break;
            case "add":
                if (gitlet.checkIfInitialized()) {
                    gitlet.makeAdd(args[1]);
                }
                break;
            case "commit":
                if (gitlet.checkIfInitialized()) {
                    gitlet.makeCommit(args[1]);
                }
                break;
            case "rm":
                if (gitlet.checkIfInitialized()) {
                    gitlet.remove(args[1]);
                }
                break;
            case "log":
                if (gitlet.checkIfInitialized()) {
                    gitlet.log();
                }
                break;
            case "global-log":
                if (gitlet.checkIfInitialized()) {
                    gitlet.globalLog();
                }
                break;
            case "find":
                if (gitlet.checkIfInitialized()) {
                    gitlet.find(args[1]);
                }
                break;
            case "status":
                if (gitlet.checkIfInitialized()) {
                    gitlet.status();
                }
                break;
            case "checkout":
                if (gitlet.checkIfInitialized()) {
                    String[] arg = Arrays.copyOfRange(args, 1, args.length);
                    if (arg.length == 1) {
                        gitlet.checkout(arg[0]);
                    } else if (arg.length == 2) {
                        File file = new File(arg[1]);
                        gitlet.checkout(file);
                    } else {
                        if (arg[1].equals("--")) {
                            gitlet.checkout(arg[0], arg[2]);
                        } else {
                            System.out.println("Incorrect operands");
                            System.exit(0);
                        }
                    }
                }
                break;
            case "branch":
                if (gitlet.checkIfInitialized()) {
                    gitlet.branch(args[1]);
                }
                break;
            case "rm-branch":
                if (gitlet.checkIfInitialized()) {
                    gitlet.rmBranch(args[1]);
                }
                break;
            case "reset":
                if (gitlet.checkIfInitialized()) {
                    gitlet.reset(args[1]);
                }
                break;
            case "merge":
                if (gitlet.checkIfInitialized()) {
                    gitlet.merge(args[1]);
                }
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
    }
}
