package project.NIR.Models.Data;

import java.io.Serial;
import java.io.Serializable;

public abstract class Data implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public abstract String getType();

}
