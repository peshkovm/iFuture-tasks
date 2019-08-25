package com.github.peshkovm.core;

import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogViewTableModel extends AbstractTableModel {

    private static final int COLUMN_COUNT = 2;
    private static final String[] COLUMN_NAMES = {"Line", "Text"};
    private static final Class<?>[] COLUMN_CLASSES = {Integer.class, String.class};

    //private final List<WeakReference<TableModelListener>> listeners = new LinkedList<>();
    private final List<Integer> linePositions = new ArrayList<>();
    private RandomAccessFile fh;
    private MappedByteBuffer buffer;
    private int fileSize;

    public LogViewTableModel() {
    }

    public void setFileFromReading(String path) throws IOException {
        linePositions.clear();
        linePositions.add(0);
        indexFile(path);
    }

    public void close() {
        if (fh != null) {
            try {
                fh.close();
            } catch (IOException ex) {
                Logger.getLogger(LogViewTableModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            fh = null;
        }
    }

    private void indexFile(String file) throws IOException {
        fh = new RandomAccessFile(file, "r");

        long n = fh.length();

        if (n > Integer.MAX_VALUE) {
            throw new IOException("File too large, " + n + " > " + Integer.MAX_VALUE);
        }

        fileSize = (int) n;

        buffer = fh.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, fileSize);

        for (int pos = 0; pos < fileSize; ++pos) {
            byte b = buffer.get();
            if (b == '\n') {
                linePositions.add(pos + 1);
            }
        }

        if (fileSize > linePositions.get(linePositions.size() - 1)) {
            linePositions.add(fileSize); // Last line without newline character.
        }
    }

    @Override
    public int getRowCount() {
        // Starts with 0 and final end position is not a row.
        return linePositions.size() - 1;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int i) {
        return COLUMN_NAMES[i];
    }

    @Override
    public Class<?> getColumnClass(int i) {
        return COLUMN_CLASSES[i];
    }

    @Override
    public boolean isCellEditable(int i, int i1) {
        return false; //ii == 1;
    }

    @Override
    public Object getValueAt(int i, int i1) {

        System.out.println("row: " + (i + 1) + " column: " + (i1 + 1));

        switch (i1) {
            case 0:
                return i + 1; //начать отчет номеров строк с 1 вместо 0
            default:
                if (0 <= i && i < getRowCount()) {
                    int startPos = linePositions.get(i);
                    int endPos = linePositions.get(i + 1) - 1;
                    byte[] line = new byte[endPos - startPos];
                    buffer.position(startPos);
                    buffer.get(line);
                    String s = new String(line, StandardCharsets.UTF_8); // UTF-8!
                    if (s.endsWith("\r")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    return s;
                }
                return "";
        }
    }

    @Override
    public void setValueAt(Object o, int i, int i1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }



/*    @Override
    public void addTableModelListener(TableModelListener tl) {
        listeners.add(new WeakReference(tl));
    }

    @Override
    public void removeTableModelListener(TableModelListener tl) {
        for (WeakReference<TableModelListener> registeredTL : listeners) {
            TableModelListener tml = registeredTL.get();
            if (tml == null || tml == tl) {
                listeners.remove(registeredTL);
                if (tml == tl) {
                    break;
                }
            }
        }
    }*/
}