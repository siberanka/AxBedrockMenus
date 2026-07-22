package com.siberanka.axbedrockmenus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class BedrockFormTextTest {
    @Test
    void usesHighContrastStylesForButtonLines() {
        assertEquals(
                "\u00A70\u00A7lVIP\u00A7r\n\u00A71Price: 1000\u00A7r",
                BedrockFormText.button("VIP\nPrice: 1000")
        );
    }

    @Test
    void removesLowContrastAndUnsafeSourceFormatting() {
        String formatted = BedrockFormText.button("\u00A77Gray &8dark <gray>light</gray>\n\u00A7kHidden");

        assertEquals(
                "\u00A70\u00A7lGray dark light\u00A7r\n\u00A71Hidden\u00A7r",
                formatted
        );
        assertFalse(formatted.matches("(?s).*\u00A7[78hik].*"));
        assertFalse(formatted.contains("<gray>"));
    }

    @Test
    void usesSeparateTitleAndContentPalettes() {
        assertEquals("\u00A70\u00A7lRanks Today\u00A7r", BedrockFormText.title("&8Ranks\nToday"));
        assertEquals(
                "\u00A7fChoose a rank\u00A7r\n\u00A7fThen confirm\u00A7r",
                BedrockFormText.content("&7Choose a rank\n&fThen confirm")
        );
    }

    @Test
    void normalizesLineBreaksAndDropsControlCharacters() {
        assertEquals(
                "\u00A7fFirst\u00A7r\n\u00A7fSecondline\u00A7r",
                BedrockFormText.content(" First\r\n\r\nSecond\u0000line ")
        );
    }

    @Test
    void keepsNormalAmpersandsAndEmptyFieldsSafe() {
        assertEquals(
                "\u00A70\u00A7lResearch & Development\u00A7r",
                BedrockFormText.button("Research & Development")
        );
        assertEquals("", BedrockFormText.title(null));
        assertEquals("", BedrockFormText.content("\n\r\n"));
    }
}
