package com.aeroseira.cbcstratagems.stratagem.input;

import com.aeroseira.cbcstratagems.stratagem.StratagemCommand;
import java.util.ArrayList;
import java.util.List;

final class PlayerStratagemInputSession {
    private final List<StratagemCommand> input = new ArrayList<>();

    PlayerStratagemInputSession() {
    }

    void add(StratagemCommand command) {
        input.add(command);
    }

    List<StratagemCommand> input() {
        return List.copyOf(input);
    }
}
