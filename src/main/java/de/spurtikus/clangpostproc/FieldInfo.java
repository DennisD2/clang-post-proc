package de.spurtikus.clangpostproc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FieldInfo {
    private String specifier; // "int"
    int offset;
}
