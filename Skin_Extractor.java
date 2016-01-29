/*
 * Skin_Extractor
 * Alain Lebret, LISIF Laboratory - PARC Group
 * Pierre and Marie Curie University, France
 * 2003
 */
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;

/**
 * This ImageJ plugin allows skin extraction in 3D color images using a
 * transformation from the RGB space to the YCbCr one.
 *
 * Y  =  0.29900 * R + 0.58700 * G + 0.11400 * B
 * Cb = -0.16874 * R - 0.33126 * G + 0.50000 * B + 0x80
 * Cr =  0.50000 * R - 0.41869 * G - 0.08131 * B + 0x80
 *
 * R = Y + 1.40200 * (Cr - 0x80)
 * G = Y - 0.34414 * (Cb - 0x80) - 0.71414 * (Cr - 0x80)
 * B = Y + 1.77200 * (Cb - 0x80)
 *
 * @version 0.1
 */
public class Skin_Extractor implements PlugInFilter {

    /** Standard Cb min threshold value */
    public static double Cb_MIN_THRESHOLD = 95;

    /** Standard Cb max threshold value */
    public static double Cb_MAX_THRESHOLD = 140;

    /** Standard Cr min threshold value */
    public static double Cr_MIN_THRESHOLD = 140;

    /** Standard Cr max threshold value */
    public static double Cr_MAX_THRESHOLD = 165;

    /** The input image to be processed */
    private ImagePlus image;

    /** The CbCr histogram */
    private ImagePlus histogram;

    public int setup(String arg, ImagePlus imp) {
        if (arg.equals("about")) {
            showAbout();
            return DONE;
        }
        this.image   = imp;
        return DOES_RGB;
    }

    /**
     * This method is called when the plugin is loaded.
     * 'arg', which may be blank, is the argument specified
     * for this plugin in IJ_Props.txt.
     */
    public void run(ImageProcessor iproc) {
        if (image == null) {
            IJ.error("No image opened");
            return;
        }

        if (!initializeMinMaxCbCr()) return;


        int width  = image.getWidth();
        int height = image.getHeight();

        int length = width * height;

        int [] _image;
        int [] _histogram;
        int [] _regions = new int[length];

        ImageStack stack = image.getStack();
        int size   = stack.getSize();
        histogram = NewImage.createRGBImage("CbCr", 256, 256, size, NewImage.FILL_WHITE);
        ImageStack histoStack = histogram.getStack();


        for (int i = 1; i <= size; i++) {
            _image = (int []) stack.getProcessor(i).getPixels();
            _histogram = (int []) histoStack.getProcessor(i).getPixels();
            computeRGBtoYCbCr(_image, _histogram, _regions);
            histoStack.getProcessor(i).flipVertical();
            for (int j = 0; j < length; j++) {
                _image[j] = (_regions[j] > 0) ? _image[j] : 0xffffffff;
            }
        }

        image.updateAndDraw();
        histogram.show();
        histogram.updateAndDraw();
    }

    /**
     * This method computes the RGB to YCbCr space transformation for all pixels.
     * @param _image the array of points to be processed
     * @param _regions the output skin mask
     */
    private void computeRGBtoYCbCr(int [] _image, int [] _histogram, int [] _regions) {
        int    width  = image.getWidth();
        int    height = image.getHeight();
        int    red, green, blue;
        double Y, Cb, Cr;
        int    pixel, position;

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                position = (row * width) + column;
                pixel = _image[position];

                red   = (pixel & 0x00FF0000) >> 16;
                green = (pixel & 0x0000FF00) >> 8;
                blue  = (pixel & 0x000000FF);
                Y     = (0.29900 * red) + (0.58700 * green) + (0.11400 * blue);
                Cb    = (-0.16874 * red) - (0.33126 * green) + (0.50000 * blue) + 0x80;
                Cr    = (0.50000 * red) - (0.41869 * green) - (0.08131 * blue) + 0x80;
                _histogram[(int)Cr * 256 + (int)Cb ] -= 100;
                _regions[position] = ((Cr > Cr_MIN_THRESHOLD) && (Cr < Cr_MAX_THRESHOLD) && (Cb > Cb_MIN_THRESHOLD) && (Cb < Cb_MAX_THRESHOLD)) ? 255 : 0;
            }
        }
    }

    private boolean initializeMinMaxCbCr() {
        GenericDialog gd = new GenericDialog("Skin Extractor Settings");
        gd.addNumericField("Cb min Threshold value: ", Skin_Extractor.Cb_MIN_THRESHOLD, 1);
        gd.addNumericField("Cb max Threshold value: ", Skin_Extractor.Cb_MAX_THRESHOLD, 1);
        gd.addNumericField("Cr min Threshold value: ", Skin_Extractor.Cr_MIN_THRESHOLD, 1);
        gd.addNumericField("Cr max Threshold value: ", Skin_Extractor.Cr_MAX_THRESHOLD, 1);
        gd.showDialog();

        if (gd.wasCanceled()) {
            IJ.error("Plugin cancelled");
            return false;
        }
        if (gd.invalidNumber()) {
            IJ.error("Invalid numeric field");
            return false;
        }

        Skin_Extractor.Cb_MIN_THRESHOLD = gd.getNextNumber();
        Skin_Extractor.Cb_MAX_THRESHOLD = gd.getNextNumber();
        Skin_Extractor.Cr_MIN_THRESHOLD = gd.getNextNumber();
        Skin_Extractor.Cr_MAX_THRESHOLD = gd.getNextNumber();
        return true;
    }

    public void showAbout() {
        IJ.showMessage("About Skin_Extractor...",
                "LISIF Laboratory, PARC Group, Pierre and Marie Curie University - 2003\n\n\n" + "Skin extractor using an YCbCr thresholding.\n");
    }
}
