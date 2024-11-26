package project.NIR.Models;

import java.io.Serial;
import java.io.Serializable;

public abstract class ServerData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public abstract String getType();

}
