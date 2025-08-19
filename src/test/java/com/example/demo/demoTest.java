package com.example.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class demoTest {

    @Test
    void testUsuarioGetters() {
        GUIFX.Usuario u = new GUIFX.Usuario("1", "1", "Diego", "CDMX", "5551234");

        assertEquals("1", u.getId());
        assertEquals("1", u.getPersonaId());
        assertEquals("Diego", u.getNombre());
        assertEquals("CDMX", u.getDireccion());
        assertEquals("5551234", u.getTelefono());
    }
}