package gitlet;


import java.io.Serializable;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.TreeMap;
import java.util.HashSet;

import static gitlet.Utils.*;

/**
 * Represents a gitlet repository.
 * does at a high level.
 *
 * @author Sergio W. Peterson
 */
public class Repository implements Serializable {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    private TreeMap<String, Branch> branches = new TreeMap<>();
    private Repository currentRep;
    private Branch currentBranch;
    private Commit head;
    private String branchName;
    private Commit split;


    public Repository() {
        boolean exits = join(GITLET_DIR, "global").exists();
        if (exits) {
            currentRep = getRepository();
        } else {
            currentRep = this;
        }
    }

    public void init() {
        if (setupPersistence()) {
            System.out.println(
                    "A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        Branch branch = new Branch("master");
        currentRep.branches.put("master", branch);
        currentRep.branchName = branch.getName();
        currentRep.currentBranch = branch;
        currentRep.head = branch.getLatestCommit();
        currentRep.currentBranch.makeCommit();
        branch.saveBranch();
        currentRep.saveRepostiroy();

    }

    public void makeAdd(String file) {
        File target = join(CWD, file);
        if (target.exists()) {
            currentRep.currentBranch.makeAdd(file);
            currentRep.saveRepostiroy();
        } else {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    public void makeCommit(String message) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        currentRep.currentBranch.makeCommit(message);
        currentRep.head = currentRep.currentBranch.getLatestCommit();
        currentRep.saveRepostiroy();
    }

    private void mergeCommit(String oldBranch, String oldID) {
        currentRep.currentBranch.mergeCommit(oldBranch, oldID);
        currentRep.head = currentRep.currentBranch.getLatestCommit();
        currentRep.saveRepostiroy();
    }

    public void remove(String file) {
        currentRep.currentBranch.remove(file);
        currentRep.saveRepostiroy();
    }

    public void log() {
        currentRep.currentBranch.log();
    }

    public void globalLog() {
        File commitFolder = new File(Commit.COMMIT_CWD.getPath());
        for (File file : commitFolder.listFiles()) {
            Commit commit = Utils.readObject(file, Commit.class);
            printlog(commit.getID(), commit.getTime(), commit.getMessage());
            System.out.println();
        }
    }

    public void find(String message) {
        currentRep.currentBranch.find(message);
    }

    public void status() {
        System.out.println("=== Branches ===");
        for (String branch : currentRep.branches.keySet()) {
            if (branch.equals(currentRep.branchName)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        currentRep.currentBranch.printStatus();
    }

    public void checkout(File file) {
        Branch holder = null;
        for (String branch : currentRep.branches.keySet()) {
            if (currentRep.branches.get(branch).hasCommit(currentRep.head.getID())) {
                holder = currentRep.branches.get(branch);
            }
        }
        if (!(holder == null)) {
            currentRep.currentBranch = holder;
            currentRep.currentBranch.checkout(file);
            currentRep.saveRepostiroy();
        } else {
            System.out.println("");
        }
    }

    public void checkout(String branch) {
        if (currentRep.currentBranch.getName().equals(branch)) {
            System.out.println("No need to checkout the current branch");
            System.exit(0);
        } else if (currentRep.branches.containsKey(branch)) {
            setBranch(branch);
            currentRep.saveRepostiroy();
        } else {
            System.out.println("No such branch exists");
            System.exit(0);
        }
    }

    public void checkout(String id, String name) {
        currentRep.currentBranch.checkout(id, name);
        currentRep.saveRepostiroy();
    }

    public void branch(String branch) {
        if (currentRep.branches.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            Branch newBranch = new Branch(branch, currentRep.currentBranch);
            currentRep.branches.put(branch, newBranch);
            currentRep.split = newBranch.getLatestCommit();
            currentRep.saveRepostiroy();
        }
    }

    public void rmBranch(String branch) {
        if (branch.equals(currentRep.currentBranch.getName())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else if (!currentRep.branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else {
            currentRep.branches.remove(branch);
            currentRep.currentBranch.delete();
            currentRep.currentBranch = currentRep.branches.get("master");
            currentRep.saveRepostiroy();
        }
    }

    public void reset(String id) {
        for (String branchID : currentRep.branches.keySet()) {
            Branch branch = currentRep.branches.get(branchID);
            branch.reset(id);
        }
        currentRep.saveRepostiroy();
    }

    public void merge(String branch) {
        if (currentRep.currentBranch.hasStuff()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        } else if (!currentRep.branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else if (currentRep.currentBranch.getName().equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        } else {
            Branch otherBranch = currentRep.branches.get(branch);
            for (File file : CWD.listFiles()) {
                if (file.getName().equals(".gitlet")
                        || file.getName().equals("gitlet")) {
                    continue;
                }
                Blob blob = new Blob(file.getName());
                if (!otherBranch.getLatestCommit().hasBlob(blob.getID())
                        && !currentRep.currentBranch.tracked(blob.getID())) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    System.exit(0);
                    return;
                }
            }
            mergeHelper(otherBranch, currentRep.split);
            mergeCommit(branch, currentRep.branches.get(branch).getLatestCommit().getID());

        }
        currentRep.saveRepostiroy();
    }

    public void test() {
        System.out.println("\nBranches Currently Present in your gitlet: ");
        for (String branchID : currentRep.branches.keySet()) {
            Branch branch = currentRep.branches.get(branchID);
            if (currentRep.currentBranch.getName().equals(branch.getName())) {
                System.out.println("Current Branch - " + branch.getName());
            } else {
                System.out.println(branch.getName());
            }
        }
        System.out.println();
        for (String branchID : currentRep.branches.keySet()) {
            Branch branch = currentRep.branches.get(branchID);
            System.out.println("In " + branch.getName() + " Branch :");
            branch.test();
        }
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Current Latest Split Point");
        System.out.println("Name : " + currentRep.split.getMessage() + " with ID :"
                + currentRep.split.getID());
        System.out.println("-----------------------------------------------------------------");
    }

    private boolean setupPersistence() {
        if (GITLET_DIR.exists()) {
            return true;
        }
        GITLET_DIR.mkdir();
        Commit.COMMIT_CWD.mkdir();
        Branch.BRANCHES_CWD.mkdir();
        return false;
    }

    private void printlog(String shar, String date, String message) {
        System.out.println("===");
        System.out.println("commit " + shar);
        System.out.println("Date: " + date);
        System.out.println(message);
    }

    private void saveRepostiroy() {
        File rep = Utils.join(GITLET_DIR, "global");
        Utils.writeObject(rep, this);
    }

    private Repository getRepository() {
        File rep = Utils.join(GITLET_DIR, "global");
        return readObject(rep, Repository.class);
    }

    private void setBranch(String branch) {
        Branch newbranch = currentRep.branches.get(branch);
        for (File file : CWD.listFiles()) {
            if (file.getName().equals(".gitlet")
                    || file.getName().equals("gitlet")) {
                continue;
            }
            Blob blob = new Blob(file.getName());
            if (!newbranch.getLatestCommit().hasBlob(blob.getID())
                    && !currentRep.currentBranch.tracked(blob.getID())) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                System.exit(0);
                return;
            }
        }
        currentRep.currentBranch = newbranch;
        currentRep.branchName = branch;
        currentRep.head = newbranch.getLatestCommit();
        currentRep.currentBranch.setFiles();
    }

    private void mergeHelper(Branch givenBranch, Commit splitpoint) {
        HashSet<String> allBlobs = getAll(givenBranch.getLatestCommit(),
                splitpoint, currentRep.currentBranch.getLatestCommit());
        Commit givenCommit = givenBranch.getLatestCommit();
        Commit currentCommit = currentRep.currentBranch.getLatestCommit();

        //Makes a hash map of the <File name, Blob ID> for all the blobs in the latest commit;
        TreeMap<String, String> givenBlobs = getNames(givenCommit);
        TreeMap<String, String> currentBlobs = getNames(currentCommit);
        TreeMap<String, String> splitBlobs = getNames(currentRep.split);
        TreeMap<String, String> newInCurrent = whatAhasbutBdoesnt(currentBlobs, splitBlobs);
        TreeMap<String, String> newInGiven = whatAhasbutBdoesnt(givenBlobs, splitBlobs);

        //For all files in the split commit
        for (String file : splitBlobs.keySet()) {

            // Checks if both the current commit has this file
            // and the the other branch has this file too
            if (currentBlobs.containsKey(file) && givenBlobs.containsKey(file)) {
                //If it was changed in the given branch but not in the current branch
                if (splitBlobs.get(file).equals(currentBlobs.get(file))
                        && !splitBlobs.get(file).equals(givenBlobs.get(file))) {

                    // Use the file from given branch
                    mergeAdd(givenCommit.getBlobs().get(givenBlobs.get(file)), givenCommit);

                    //Changed in the current branch but not in the given branch
                } else if (!splitBlobs.get(file).equals(currentBlobs.get(file))
                        && splitBlobs.get(file).equals(givenBlobs.get(file))) {

                    // Use the file from current branch
                    mergeAdd(currentCommit.getBlobs().get(currentBlobs.get(file)), givenCommit);

                    //If it was changed in both the given and the current branch to the same thing
                } else if (!splitBlobs.get(file).equals(currentBlobs.get(file))
                        && !splitBlobs.get(file).equals(givenBlobs.get(file))
                        && currentBlobs.get(file).equals(givenBlobs.get(file))) {

                    // Use the file from current branch
                    mergeAdd(currentCommit.getBlobs().get(currentBlobs.get(file)), givenCommit);

                    //If it was changed in both given and current branch to diffrent things
                } else if (!splitBlobs.get(file).equals(currentBlobs.get(file))
                        && !splitBlobs.get(file).equals(givenBlobs.get(file))
                        && !currentBlobs.get(file).equals(givenBlobs.get(file))) {

                    //Merge Conflict for the two files
                    mergerConflict(currentCommit.getBlobs().get(currentBlobs.get(file)),
                            givenCommit.getBlobs().get(givenBlobs.get(file)));
                }

                // If the file is in the current commit but not in the given commit
            } else if (currentBlobs.containsKey(file) && !givenBlobs.containsKey(file)) {
                if (currentBlobs.get(file).equals(splitBlobs.get(file))) {
                    remove(file);
                }
            } else if (!currentBlobs.containsKey(file) && givenBlobs.containsKey(file)) {
                if (givenBlobs.get(file).equals(splitBlobs.get(file))) {
                    continue;
                }
            }
        }
        for (String file : newInCurrent.keySet()) {
            if (!newInGiven.containsKey(file)) {
                mergeAdd(currentCommit.getBlobs().get(currentBlobs.get(file)), currentCommit);
            }
        }
        for (String file : newInGiven.keySet()) {
            if (!newInCurrent.containsKey(file)) {
                mergeAdd(givenCommit.getBlobs().get(givenBlobs.get(file)), givenCommit);
            }
        }
    }

    private void mergeAdd(Blob file, Commit other) {
        //Blob we want to add to the current new branch
        TreeMap<String, Blob> otherBlobs = other.getBlobs();
        TreeMap<String, Blob> currentBlobs = currentRep.currentBranch.getLatestCommit().getBlobs();
        //If the file is already in the currents commit
        if (currentBlobs.containsKey(file.getID())) {
            return;
            // If the file is not in the current commit but is in the other commit
        } else if (!currentBlobs.containsKey(file.getID())
                && otherBlobs.containsKey(file.getID())) {

            //Return the file from the other commit
            checkout(other.getID(), file.getName());
        } else {
            return;
        }
    }


    private TreeMap<String, String> whatAhasbutBdoesnt(TreeMap<String, String> commit1,
                                                       TreeMap<String, String> commit2) {
        TreeMap<String, String> holder = new TreeMap<>();
        for (String file : commit1.keySet()) {
            if (!commit2.containsKey(file)) {
                holder.put(file, commit1.get(file));
            }
        }
        return holder;
    }

    private TreeMap<String, String> getNames(Commit commit) {
        TreeMap<String, String> holder = new TreeMap<>();
        TreeMap<String, Blob> old = commit.getBlobs();
        for (String blob : old.keySet()) {
            holder.put(old.get(blob).getName(), blob);
        }
        return holder;
    }

    private HashSet<String> getAll(Commit a, Commit b, Commit c) {
        HashSet<String> allNames = new HashSet<>();
        TreeMap<String, String> filesA = new TreeMap<>(getNames(a));
        TreeMap<String, String> filesB = new TreeMap<>(getNames(b));
        TreeMap<String, String> filesC = new TreeMap<>(getNames(c));
        allNames.addAll(filesA.keySet());
        allNames.addAll(filesB.keySet());
        allNames.addAll(filesC.keySet());
        return allNames;
    }


    private void mergerConflict(Blob current, Blob given) {
        try {
            File conflicFile = new File(CWD, current.getName());
            conflicFile.delete();
            OutputStream os = new FileOutputStream(conflicFile, true);
            String headText = "<<<<<<< HEAD\n";
            String divider = "\n=======\n";
            String endText = "\n>>>>>>>";
            os.write(headText.getBytes(), 0, headText.length());
            os.write(current.getFile(), 0, current.getFile().length);
            os.write(divider.getBytes(), 0, divider.length());
            os.write(given.getFile(), 0, given.getFile().length);
            os.write(endText.getBytes(), 0, endText.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkIfInitialized() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
            return false;
        }
        return true;
    }
}
