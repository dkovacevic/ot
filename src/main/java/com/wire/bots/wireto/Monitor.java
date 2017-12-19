package com.wire.bots.wireto;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.OT;
import ot.internal.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: dejankovacevic
 * Date: 18/12/17
 * Time: 17:49
 */
class Monitor {
    private static final String FILENAME = "document.txt";
    private final ClientRepo repo;
    private final HashSet<String> convs = new HashSet<>();

    Monitor(ClientRepo repo) {
        this.repo = repo;
    }

    void addMonitor(String botId, String convId, final Text document) {
        if (convs.add(botId)) {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    WireClient wireClient = null;
                    try {
                        String newDoc = readFile();

                        if (newDoc.equalsIgnoreCase(document.toString()))
                            return;

                        wireClient = repo.getWireClient(botId, convId);

                        Changes diff = document.diff(Text.wrap(newDoc));

                        //We want to make sure first operation is Retain always
                        if (!diff.getChanges().isEmpty()) {
                            Change change = diff.getChanges().get(0);
                            if (!(change instanceof Retain)) {
                                wireClient.sendOT(OT.Operation.RETAIN, "", 0);
                            }
                        }

                        for (Change change : diff.getChanges()) {
                            if (change instanceof Insert) {
                                Insert insert = (Insert) change;
                                wireClient.sendOT(OT.Operation.INSERT, insert.getText(), insert.getText().length());
                            }
                            if (change instanceof Delete) {
                                Delete delete = (Delete) change;
                                wireClient.sendOT(OT.Operation.DELETE, "", delete.getLen());
                            }
                            if (change instanceof Retain) {
                                Retain retain = (Retain) change;
                                wireClient.sendOT(OT.Operation.RETAIN, "", retain.getLength());
                            }
                        }

                        String before = document.toString();
                        document.apply(diff);
                        Logger.info(String.format("Before: '%s', '%s', After: '%s'",
                                before,
                                diff,
                                document));

                    } catch (Exception e) {
                        Logger.error(e.toString());
                        if(wireClient != null)
                            try {
                                wireClient.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                    }
                }
            }, 1000, 1000);
        }
    }

    private String readFile() throws URISyntaxException, IOException {
        return new String(Files.readAllBytes(Paths.get(FILENAME)));
    }

}
