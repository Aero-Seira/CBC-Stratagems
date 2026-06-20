package com.aeroseira.cbcstratagems.stratagem.input;

import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import java.util.ArrayList;
import java.util.List;

final class PlayerStratagemInputSession {
    private final List<StratagemCommand> input = new ArrayList<>();
    private boolean inputBlocked;

    PlayerStratagemInputSession(boolean inputBlocked) {
        this.inputBlocked = inputBlocked;
    }

    void add(StratagemCommand command) {
        input.add(command);
    }

    void clear() {
        input.clear();
    }

    List<StratagemCommand> input() {
        return List.copyOf(input);
    }

    boolean inputBlocked() {
        return inputBlocked;
    }

    void setInputBlocked(boolean inputBlocked) {
        this.inputBlocked = inputBlocked;
    }
}
