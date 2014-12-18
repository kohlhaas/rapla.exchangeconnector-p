package org.rapla.plugin.exchangeconnector;

import java.util.ArrayList;
import java.util.List;

public class SynchronizeResult {
    public int changed;
    public int removed;
    public int open;
    public List<String> errorMessages = new ArrayList<String>();
}
