package net.bible.service.format.osistohtml;

import net.bible.service.common.Logger;

import org.xml.sax.Attributes;

/** Paragraph <p>...</p>
 * 
 * @author Martin Denham [mjdenham at gmail dot com]
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's author. 
 */
public class TransChangeHandler implements OsisTagHandler {

	private HtmlTextWriter writer;
	
	@SuppressWarnings("unused")
	private OsisToHtmlParameters parameters;
	
	@SuppressWarnings("unused")
	private static final Logger log = new Logger("LHandler");

	public TransChangeHandler(OsisToHtmlParameters parameters, HtmlTextWriter writer) {
		this.parameters = parameters;
		this.writer = writer;
	}
	
	@Override
	public String getTagName() {
        return OSISUtil2.OSIS_ELEMENT_TRANSCHANGE;
    }

	@Override
	public void start(Attributes attrs) {
		writer.write("<span class='transChange'>");
	}

	@Override
	public void end() {
		writer.write("</span>");
	}
}
