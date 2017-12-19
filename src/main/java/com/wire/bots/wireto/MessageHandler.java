package com.wire.bots.wireto;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.OTMessage;
import com.wire.bots.sdk.models.TextMessage;
import ot.internal.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class MessageHandler extends MessageHandlerBase {
    private static final String FILENAME = "document.txt";
    private Text document = Text.empty();
    private final Monitor monitor;

    MessageHandler(ClientRepo repo) {
        monitor = new Monitor(repo);
    }

    @Override
    public void onOT(WireClient client, OTMessage otMessage) {
        try {
            Change change = null;
            switch (otMessage.getOperation()) {
                case RETAIN:
                    change = new Retain(otMessage.getOffset());
                    break;
                case INSERT:
                    change = new Insert(otMessage.getText());
                    break;
                case DELETE:
                    change = new Delete(otMessage.getOffset());
                    break;
            }

            String before = document.toString();
            document.apply(change, document.getPos());
            if (!(change instanceof Delete))
                document.setPos(change.offset());

            writeFile(document.toString());
            monitor.addMonitor(client.getId(), client.getConversationId(), document);

            Logger.info(String.format("Before: '%s', '%s', After: '%s'", before, change, document));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            if (msg.getText().startsWith("/file")) {
                monitor.addMonitor(client.getId(), client.getConversationId(), document);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeFile(String content) throws IOException {
        try (FileWriter fw = new FileWriter(FILENAME, false)) {
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(content);
            }
        }
    }

}
