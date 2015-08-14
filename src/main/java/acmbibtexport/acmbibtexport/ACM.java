/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package acmbibtexport.acmbibtexport;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author Heleno
 */
public class ACM {

    final WebClient webClient;
    static int refCount = 0;
    private JTextArea console;

    public ACM(JTextArea console) {
        webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_11);
        this.console = console;
    }

    public HtmlPage getPage(final String url) {
        
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    console.append("\n Loading page: " + url);
                }
            });
        try {
            return webClient.getPage(url);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Unexpected error acessing: " + url);
        } catch (FailingHttpStatusCodeException ex) {
            JOptionPane.showMessageDialog(null, "Unexpected error acessing: " + url);
        }
        return null;
    }

    public void navigate(String url) {
        System.out.println(console.getText());
        console.append("\n Acessing ACM....");
        HtmlPage page1 = getPage(url);
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(java.util.logging.Level.OFF);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        console.append("\n ACM page loaded.");

        File f = new File("acm.bib");
        
        try {
            OutputStream os = new FileOutputStream(f);
            HtmlAnchor nextPage = null;
            do {
                List<HtmlAnchor> links = page1.getAnchors();
                nextPage = fetchLinksToFile(os, links);
                if (nextPage != null) {
                    console.append("\n Next Page");
                    String nextPageLink = "http://dl.acm.org/".concat(nextPage.getHrefAttribute());
                    page1 = getPage(nextPageLink);
                }
            } while (nextPage != null);
            os.close();
            console.append("\n\n\nBibTeX successfully exported. Please check the root folder of this app for the file acm.bib");
        } catch (FileNotFoundException ex) {
            System.out.println("File not found");
        }catch(IOException ex){
            System.out.println("IO Exception");
        }

    }

    private HtmlAnchor fetchLinksToFile(OutputStream os, List<HtmlAnchor> links) throws IOException {
        HtmlAnchor nextPage = null;
        for (HtmlAnchor link : links) {
            if (link.getHrefAttribute().contains("citation.cfm")) {
//                System.out.println(link.getHrefAttribute());
//                link.setAttribute("href", "http://dl.acm.org/"+link.getHrefAttribute());
//                System.out.println(link.getHrefAttribute());
                HtmlPage page2 = getPage("http://dl.acm.org/".concat(link.getHrefAttribute()));

                try {
                    HtmlAnchor bibLink = page2.getAnchorByText("BibTeX");
                    String exportLink = bibLink.getHrefAttribute();
                    int initialIndex = exportLink.indexOf("exportformats");

                    int lastIndex = exportLink.indexOf("'", initialIndex);
//                    System.out.println(exportLink.substring(initialIndex, lastIndex));
                    exportLink = "http://dl.acm.org/".concat(exportLink.substring(initialIndex, lastIndex));
//                    System.out.println(exportLink);
                    page2 = getPage(exportLink);
                    try {
                        InputStream is = page2.getAnchorByText("download").click().getWebResponse().getContentAsStream();
                        console.append("\n "+ ++refCount + "   GETTING REFERENCE OF:                 " + link.getTextContent());
                        int read = 0;
                        try {
                            byte[] bytes = new byte[1024]; // make it bigger if you want. Some recommend 8x, others 100x
                            while ((read = is.read(bytes)) != -1) {
                                os.write(bytes, 0, read);
                                os.write("\n".getBytes());
                            }

                            is.close();
                        } catch (IOException ex) {
                            // Exception handling
                        }
                    } catch (IOException ex) {
                        // Exception handling
                    }
                } catch (ElementNotFoundException e) {
                    // exception
                    System.out.println("Bibtex link not found.");
                }
            } else if (link.getHrefAttribute().contains("results.cfm")) {
                if (link.getTextContent().contains("next")) {
                    nextPage = link;
                }

            }
        }
        return nextPage;
    }
}
