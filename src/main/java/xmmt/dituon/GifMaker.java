package xmmt.dituon;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.RenderedImage;
import java.io.*;

public class GifMaker {

    protected ImageWriter writer;
    protected ImageWriteParam params;
    protected IIOMetadata metadata;
    protected ImageOutputStream image;
    protected InputStream output;

    public GifMaker(int imageType, int delay, boolean loop) throws IOException {
        writer = ImageIO.getImageWritersBySuffix("gif").next();
        params = writer.getDefaultWriteParam();

        ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
        metadata = writer.getDefaultImageMetadata(imageTypeSpecifier, params);

        configureRootMetadata(delay, loop);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        image = ImageIO.createImageOutputStream(os);
        writer.setOutput(image);
        writer.prepareWriteSequence(null);
    }

    public byte[] getBytes(ImageOutputStream ios) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(255);
        long counter = 0;
        try {
            System.out.println("getStreamPosition()[BEFORE]=" + ios.getStreamPosition());
            ios.seek(0);
            System.out.println("getStreamPosition()[AFTER]=" + ios.getStreamPosition());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        while (true) {
            try {
                bos.write(ios.readByte());
                counter++;
            } catch (EOFException e) {
                System.out.println("End of Image Stream");
                break;
            } catch (IOException e) {
                System.out.println("Error processing the Image Stream");
                break;
            }
        }
        System.out.println("Total bytes read=" + counter);
        return bos.toByteArray();
    }

    private void configureRootMetadata(int delay, boolean loop) throws IIOInvalidTreeException {
        String metaFormatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(metaFormatName);

        IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
        graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
        graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
        graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(delay / 10));
        graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

        IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
        IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
        child.setAttribute("applicationID", "NETSCAPE");
        child.setAttribute("authenticationCode", "2.0");

        int loopContinuously = loop ? 0 : 1;
        child.setUserObject(new byte[]{ 0x1, (byte) (loopContinuously & 0xFF), (byte) ((loopContinuously >> 8) & 0xFF)});
        appExtensionsNode.appendChild(child);
        metadata.setFromTree(metaFormatName, root);
    }

    private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName){
        int nNodes = rootNode.getLength();
        for (int i = 0; i < nNodes; i++){
            if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)){
                return (IIOMetadataNode) rootNode.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        rootNode.appendChild(node);
        return(node);
    }

    public void writeToSequence(RenderedImage img) throws IOException {
        writer.writeToSequence(new IIOImage(img, null, metadata), params);
    }

    public InputStream getOutput(){
        return output;
    }

    public void close() throws IOException {
        writer.endWriteSequence();
        output = new ByteArrayInputStream(getBytes(image));
    }
}