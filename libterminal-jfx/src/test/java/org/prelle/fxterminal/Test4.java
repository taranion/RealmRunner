package org.prelle.fxterminal;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

public class Test4 {

    // Ersetzen Sie "IhrFontPfad.ttf" durch den tatsächlichen Pfad zu Ihrer TTF-Datei
    //private static final String FONT_PATH = "src/main/resources/AcPlus_IBM_VGA_9x16-2x.ttf"; 
    private static final String FONT_PATH = "/var/lib/snapd/snap/telegram-desktop/6474/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf";

    public static void main(String[] args) {
        
        Font font = null;
        try {
            // 1. Laden des TTF-Fonts aus einer Datei
            File fontFile = new File(FONT_PATH);
            font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 32f);
            
            // Registrieren des Fonts in der GraphicsEnvironment, optional, aber gut
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);

        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Font-Datei: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Fehler beim Erstellen des Fonts: " + e.getMessage());
            return;
        }
        
        if (font == null) {
            System.out.println("Font konnte nicht geladen werden.");
            return;
        }

        // 2. Erhalten der FontMetrics
        // FontMetrics benötigen einen Rendering-Kontext (Toolkit) zur genauen Berechnung
        FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);

        // 3. Ausgabe der Metriken
        System.out.println("-------------------------------------");
        System.out.println("Font-Metriken für: " + font.getFontName() + " (Größe: " + font.getSize() + "pt)");
        System.out.println("-------------------------------------");

        // Grundlegende Metriken
        System.out.println("1. Gesamt-Höhe der Zeile (Height): " + metrics.getHeight() + " Pixel");
        System.out.println("   (Summe aus Ascent + Descent + Leading)");
        System.out.println("2. Ascent (Aufstieg über die Baseline): " + metrics.getAscent() + " Pixel");
        System.out.println("3. Descent (Absinken unter die Baseline): " + metrics.getDescent() + " Pixel");
        System.out.println("4. Leading (Zwischenraum zwischen Zeilen): " + metrics.getLeading() + " Pixel");
        
        // Metrik für die Breite eines Strings
        String testString = "Hallo Welt!";
        int stringWidth = metrics.stringWidth(testString);
        System.out.println("5. Stringbreite ('" + testString + "'): " + stringWidth + " Pixel");

        // Maximale Metriken (über alle Glyphen im Font)
        System.out.println("6. Max. Ascent: " + metrics.getMaxAscent() + " Pixel");
        System.out.println("7. Max. Descent: " + metrics.getMaxDescent() + " Pixel");
        System.out.println("8. Max. Advance (max. Zeichenbreite): " + metrics.getMaxAdvance() + " Pixel");
        System.out.println("-------------------------------------");
    }
}