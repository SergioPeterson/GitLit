package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.HashSet;

/**
 * Represents a gitlet commit object.
 * does at a high level.
 *
 * @author Sergio W. Peterson
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /**
     * The message of this Commit.
     */
    public static final File COMMIT_CWD = Utils.join(Repository.GITLET_DIR, "commits");

    private String message;
    private String time;
    private String id;
    private Commit parent;
    private TreeMap<String, Blob> blobs = new TreeMap<>();
    private static final Date STARTDATE = new Date(0);
    SimpleDateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");


    //only used when initaing gitlet
    public Commit() {
        this.message = "initial commit";
        this.time = format.format(STARTDATE);
        this.parent = null;
        this.id = Utils.sha1(this.time, this.message);
    }

    public Commit(String message, Commit parent) {
        this.message = message;
        Date current = new Date();
        this.time = format.format(current);
        this.blobs = new TreeMap<>(parent.getBlobs());
        this.parent = parent;
        this.id = Utils.sha1(this.time.toString(), this.message);
    }

    public void saveCommit() {
        File newCommit = Utils.join(COMMIT_CWD, this.id);
        Utils.writeObject(newCommit, this);

    }

    public void addBlobs(TreeMap<String, Blob> blob, TreeMap<String, Blob> withhold) {
        refactor(blob, withhold);
    }

    public TreeMap<String, Blob> getBlobs() {
        return blobs;
    }

    public String getID() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public Commit getParent() {
        return parent;
    }

    public void info() {
        System.out.println("\t  " + this.message);
        System.out.println("\t   ID : " + this.id);
        System.out.println("\t   Time : " + this.time);
        System.out.println("\t   Blobs for this Commit : ");
        for (String blob : blobs.keySet()) {
            Blob file = blobs.get(blob);
            System.out.println("\t   - Name : " + file.getName() + " ID: " + file.getID());
        }
        System.out.println();
    }

    public boolean hasBlob(String thisid) {
        return blobs.containsKey(thisid);
    }

    private void refactor(TreeMap<String, Blob> newBlobs, TreeMap<String, Blob> withhold) {
        TreeMap<String, String> holderofBlobs = new TreeMap<>();
        HashSet<String> removeBlobs = new HashSet<>();
        for (String oldBlob : this.blobs.keySet()) {
            Blob thisblob = this.blobs.get(oldBlob);
            if (withhold.containsKey(oldBlob)) {
                removeBlobs.add(oldBlob);
            }
            holderofBlobs.put(thisblob.getName(), thisblob.getID());
        }

        for (String remove : removeBlobs) {
            this.blobs.remove(remove);
        }


        for (String blob : newBlobs.keySet()) {
            if (holderofBlobs.containsKey(newBlobs.get(blob).getName())) {
                String thisid = holderofBlobs.get(newBlobs.get(blob).getName());
                this.blobs.remove(thisid);
            }
            this.blobs.put(blob, newBlobs.get(blob));
        }
    }


    public void resetTo(TreeMap<String, Blob> newblob) {
        this.blobs = newblob;
    }

}
