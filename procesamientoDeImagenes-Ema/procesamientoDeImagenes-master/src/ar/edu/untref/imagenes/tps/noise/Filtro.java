package ar.edu.untref.imagenes.tps.noise;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

public class Filtro implements BufferedImageOp, RasterOp {

	public static final int PIXELES_BORDE_EN_CERO = 0;

	private Kernel kernel;
	private int condicionBorde;
	private RenderingHints hints;

	public Filtro(Kernel kernel) {
		this.kernel = kernel;
		condicionBorde = PIXELES_BORDE_EN_CERO;
	}

	public final BufferedImage filter(BufferedImage imagenOriginal, BufferedImage imagenDestino) {
		if (imagenOriginal == imagenDestino)
			throw new IllegalArgumentException("La imagen original y la de destino no pueden ser las mismas");

		if (imagenDestino == null)
			imagenDestino = createCompatibleDestImage(imagenOriginal, imagenOriginal.getColorModel());

		BufferedImage src1 = imagenOriginal;
		BufferedImage dst1 = imagenDestino;
		
		if (src1.getColorModel().getColorSpace().getType() != imagenDestino
				.getColorModel().getColorSpace().getType())
			dst1 = createCompatibleDestImage(imagenOriginal, imagenOriginal.getColorModel());

		filter(src1.getRaster(), dst1.getRaster());

		return imagenDestino;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src,
			ColorModel modeloDeColoor) {
		if (modeloDeColoor != null)
			return new BufferedImage(modeloDeColoor, src.getRaster()
					.createCompatibleWritableRaster(),
					src.isAlphaPremultiplied(), null);

		return new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
	}

	public final RenderingHints getRenderingHints() {
		return hints;
	}

	public int getCondicionBorde() {
		return condicionBorde;
	}

	public final Kernel getMascara() {
		return (Kernel) kernel.clone();
	}

	public final WritableRaster filter(Raster imagenInicial, WritableRaster imagenDestino) {
		if (imagenInicial == imagenDestino)
			throw new IllegalArgumentException("imagen origen y destino deben ser distintas");
		if (kernel.getWidth() > imagenInicial.getWidth()
				|| kernel.getHeight() > imagenInicial.getHeight())
			throw new ImagingOpException("La m�scara es muy grande");
		if (imagenDestino == null)
			imagenDestino = createCompatibleDestRaster(imagenInicial);
		else if (imagenInicial.getNumBands() != imagenDestino.getNumBands())
			throw new ImagingOpException(
					"imagen origen y destino tienen distinto numero de bandas");

		int anchoMascara = kernel.getWidth();
		int alturaMascara = kernel.getHeight();
		int izquierda = kernel.getXOrigin();
		int derecha = Math.max(anchoMascara - izquierda - 1, 0);
		int arriba = kernel.getYOrigin();
		int abajo = Math.max(alturaMascara - arriba - 1, 0);

		//Magia de buffered image
		int[] valorMaximo = imagenInicial.getSampleModel().getSampleSize();
		for (int i = 0; i < valorMaximo.length; i++)
			valorMaximo[i] = (int) Math.pow(2, valorMaximo[i]) - 1;

		int anchoDeLaRegionAlcanzable = imagenInicial.getWidth() - izquierda - derecha;
		int altoDeLaRegionAlcanzable = imagenInicial.getHeight() - arriba - abajo;
		float[] valoresDeLaMascara = kernel.getKernelData(null);
		float[] matrizTemporal = new float[anchoMascara * alturaMascara];

		float[]maximosYMinimos = calcularMaximosYMinimos(imagenInicial, imagenDestino);
		float minimo = maximosYMinimos[0];
		float maximo = maximosYMinimos[1];
		
		for (int x = 0; x < anchoDeLaRegionAlcanzable; x++) {
			for (int y = 0; y < altoDeLaRegionAlcanzable; y++) {

				for (int banda = 0; banda < imagenInicial.getNumBands(); banda++) {
					float v = 0;
					imagenInicial.getSamples(x, y, anchoMascara, alturaMascara, banda, matrizTemporal);
					for (int i = 0; i < matrizTemporal.length; i++)
						v += matrizTemporal[matrizTemporal.length - i - 1] * valoresDeLaMascara[i];

					/*Truncado
					if (v > valorMaximo[banda])
						v = valorMaximo[banda];
					else if (v < 0)
						v = 0;*/
					
					float vTransformado = ((((float)valorMaximo[banda]) / (maximo - minimo)) * v) - ((minimo * (float)valorMaximo[banda]) / (maximo - minimo));

					imagenDestino.setSample(x + kernel.getXOrigin(), y + kernel.getYOrigin(), banda, vTransformado);
					
				}
			}
		}

		return imagenDestino;
	}
	
	public WritableRaster createCompatibleDestRaster(Raster src) {
		return src.createCompatibleWritableRaster();
	}

	public final Rectangle2D getBounds2D(BufferedImage src) {
		return src.getRaster().getBounds();
	}

	public final Rectangle2D getBounds2D(Raster src) {
		return src.getBounds();
	}

	public final Point2D getPoint2D(Point2D src, Point2D dst) {
		if (dst == null)
			return (Point2D) src.clone();
		dst.setLocation(src);
		return dst;
	}
	
	public float[] calcularMaximosYMinimos(Raster imagenInicial, WritableRaster imagenDestino){
		
		int anchoMascara = kernel.getWidth();
		int alturaMascara = kernel.getHeight();
		int izquierda = kernel.getXOrigin();
		int derecha = Math.max(anchoMascara - izquierda - 1, 0);
		int arriba = kernel.getYOrigin();
		int abajo = Math.max(alturaMascara - arriba - 1, 0);
		
		float maximo = 0;
		float minimo = 0;

		int anchoDeLaRegionAlcanzable = imagenInicial.getWidth() - izquierda - derecha;
		int altoDeLaRegionAlcanzable = imagenInicial.getHeight() - arriba - abajo;
		float[] valoresDeLaMascara = kernel.getKernelData(null);
		float[] matrizTemporal = new float[anchoMascara * alturaMascara];

		for (int x = 0; x < anchoDeLaRegionAlcanzable; x++) {
			for (int y = 0; y < altoDeLaRegionAlcanzable; y++) {

				for (int banda = 0; banda < imagenInicial.getNumBands(); banda++) {
					float v = 0;
					imagenInicial.getSamples(x, y, anchoMascara, alturaMascara, banda, matrizTemporal);
					for (int i = 0; i < matrizTemporal.length; i++)
						v += matrizTemporal[matrizTemporal.length - i - 1] * valoresDeLaMascara[i];
					
					if (maximo < v){
						maximo = v;
					}
					
					if(minimo > v){
						minimo = v;
					}
				}
			}
		}
		float[] maximosYMinimos = new float[2];
		maximosYMinimos[0] = minimo;
		maximosYMinimos[1] = maximo;
		return maximosYMinimos;
	}
}