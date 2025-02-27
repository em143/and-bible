package net.bible.android.control.page;

import net.bible.android.activity.R;
import net.bible.android.control.ControlFactory;
import net.bible.android.view.activity.navigation.GridChoosePassageBook;
import net.bible.service.common.CommonUtils;
import net.bible.service.sword.SwordDocumentFacade;

import org.apache.commons.lang.StringUtils;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookCategory;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.KeyUtil;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.crosswire.jsword.passage.Verse;
import org.crosswire.jsword.versification.BibleBook;
import org.crosswire.jsword.versification.Versification;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/** Reference to current passage shown by viewer
 * 
 * @author Martin Denham [mjdenham at gmail dot com]
 * @see gnu.lgpl.License for license details.<br>
 *      The copyright to this program is held by it's author.
 */
public class CurrentBiblePage extends VersePage implements CurrentPage {
	
	private static final String TAG = "CurrentBiblePage";
	
	
	/* default */ CurrentBiblePage(CurrentBibleVerse currentBibleVerse) {
		super(true, currentBibleVerse);
	}

	public BookCategory getBookCategory() {
		return BookCategory.BIBLE;
	}

	public Class<? extends Activity> getKeyChooserActivity() {
		return GridChoosePassageBook.class;
	}
	
	/* (non-Javadoc)
	 * @see net.bible.android.control.CurrentPage#next()
	 */
	@Override
	public void next() {
		Log.d(TAG, "Next");
		nextChapter();
	}
	/* go to prev verse quietly without updates
	 */
	public void doPreviousVerse() {
		Log.d(TAG, "Previous verse");

		Versification versification = getVersification();
		Verse verse = getCurrentBibleVerse().getVerseSelected(versification);
		getCurrentBibleVerse().setVerseSelected(versification, getBibleTraverser().getPrevVerse(getCurrentPassageBook(), verse));
	}
	
	/* go to next verse quietly without updates
	 */
	public void doNextVerse() {
		Log.d(TAG, "Next verse");
		Versification versification = getVersification();
		Verse verse = getCurrentBibleVerse().getVerseSelected(versification);
		getCurrentBibleVerse().setVerseSelected(versification, getBibleTraverser().getNextVerse(getCurrentPassageBook(), verse));
	}
	
	/* (non-Javadoc)
	 * @see net.bible.android.control.CurrentPage#previous()
	 */
	@Override
	public void previous() {
		Log.d(TAG, "Previous");
		previousChapter();
	}

	private void nextChapter() {
		setKey( getKeyPlus(+1) );
	}
	
	private void previousChapter() {
		setKey( getKeyPlus(-1) );
	}

	/** add or subtract a number of pages from the current position and return Verse
	 */
	public Verse getKeyPlus(int num) {
		Verse currVer = this.getCurrentBibleVerse().getVerseSelected(getVersification());

		try {
			Verse nextVer = currVer;
			if (num>=0) {
				// move to next book if required
				for (int i=0; i<num; i++) {
					nextVer = getBibleTraverser().getNextChapter(getCurrentPassageBook(), nextVer);
				}
			} else {
				// move to prev book if required
				// allow standard loop structure by changing num to positive
				num = -num;
				for (int i=0; i<num; i++) {
					nextVer = getBibleTraverser().getPrevChapter(getCurrentPassageBook(), nextVer);
				}
			}
		
			return nextVer;
		} catch (Exception nsve) {
			Log.e(TAG, "Incorrect verse", nsve);
			return currVer;
		}
	}
	
	/** add or subtract a number of pages from the current position and return Page
	 */
	public Key getPagePlus(int num) {
		Verse targetChapterVerse1 = getKeyPlus(num);

		// convert to full chapter before returning because bible view is for a full chapter
		return CommonUtils.getWholeChapter(targetChapterVerse1);
	}

	/* (non-Javadoc)
	 * @see net.bible.android.control.CurrentPage#setKey(java.lang.String)
	 */
	public void setKey(String keyText) {
		Log.d(TAG, "key text:"+keyText);
		try {
			Key key = getCurrentDocument().getKey(keyText);
			setKey(key);
		} catch (NoSuchKeyException nske) {
			Log.e(TAG, "Invalid verse reference:"+keyText);
		}
	}

	
	/** set key without notification
	 * 
	 * @param key
	 */
	public void doSetKey(Key key) {
		if (key!=null) {
			Verse verse = KeyUtil.getVerse(key);
			//TODO av11n should this be the verse Versification or the Module/doc's Versification
			getCurrentBibleVerse().setVerseSelected(getVersification(), verse);
		}
	}

	/* (non-Javadoc)
	 * @see net.bible.android.control.CurrentPage#getSingleKey()
	 */
	@Override
	public Verse getSingleKey() {
		Key key = doGetKey(true);
		// it is already a Verse but this avoids a downcast
		return KeyUtil.getVerse(key);
    }
	
	/* (non-Javadoc)
	 * @see net.bible.android.control.CurrentPage#getKey()
	 */
	@Override
	public Key getKey() {
		return doGetKey(false);
    }

	private Key doGetKey(boolean requireSingleKey) {
		Verse verse = getCurrentBibleVerse().getVerseSelected(getVersification());
		if (verse!=null) {
			Key key;
			if (!requireSingleKey) {
				// display whole page of bible so return whole chapter key - not just the single verse even if a single verse was set in verseKey
				// if verseNo is required too then use getVerse()
		        key = CommonUtils.getWholeChapter(verse);
			} else {
				key = verse;
			}
			return key;
		} else {
			Log.w(TAG, "No verse, returning default verse Gen 1.1");
			return new Verse(getVersification(), BibleBook.GEN,1,1, true);
		}
    }

	@Override
	public boolean isSingleKey() {
		return false;
	}
	
	public int getCurrentVerseNo() {
		return getCurrentBibleVerse().getVerseNo();
	}
	public void setCurrentVerseNo(int verse) {
		getCurrentBibleVerse().setVerseNo(verse);
		onVerseChange();
	}

	/** called during app close down to save state
	 * 
	 * @param outState
	 */
	@Override
	public JSONObject getStateJson() throws JSONException {
		JSONObject object = new JSONObject();
		if (getCurrentDocument()!=null && getCurrentBibleVerse()!=null && getCurrentBibleVerse().getVerseSelected(getVersification())!=null) {
			Log.d(TAG, "Saving Bible state for 1 window");
			object.put("document", getCurrentDocument().getInitials());
			object.put("verse", getCurrentBibleVerse().getStateJson());
		}
		return object;
	}
	
	/** called during app start-up to restore previous state
	 * 
	 * @param inState
	 */
	@Override
	public void restoreState(JSONObject jsonObject) throws JSONException {
		if (jsonObject!=null) {
			Log.d(TAG, "Restoring Bible page state");
			if (jsonObject.has("document")) {
				String document = jsonObject.getString("document");
				if (StringUtils.isNotEmpty(document)) {
					Log.d(TAG, "State document:"+document);
					Book book = SwordDocumentFacade.getInstance().getDocumentByInitials(document);
					if (book!=null) {
						Log.d(TAG, "Restored document:"+book.getName());
						// bypass setter to avoid automatic notifications
						localSetCurrentDocument(book);
						
						getCurrentBibleVerse().restoreState(jsonObject.getJSONObject("verse"));
					}
				}
			}
		}
	}

	/** can we enable the main menu search button 
	 */
	@Override
	public boolean isSearchable() {
		try {
			//TODO allow japanese search - japanese bibles use smartcn which is not available
			return !"ja".equals(getCurrentDocument().getLanguage().getCode());
		} catch (Exception e) {
			Log.w(TAG,  "Missing language code", e);
			return true;
		}
	}

	@Override
	public void updateContextMenu(Menu menu) {
		super.updateContextMenu(menu);
		// by default disable notes but bible will enable
		menu.findItem(R.id.notes).setVisible(true);
		
		// by default disable mynotes but bible and commentary will enable
		MenuItem myNotesMenuItem = menu.findItem(R.id.myNoteAddEdit);
		myNotesMenuItem.setVisible(true);
		myNotesMenuItem.setTitle(ControlFactory.getInstance().getMyNoteControl().getAddEditMenuText());

		// by default disable compare translation except for Bibles
		menu.findItem(R.id.compareTranslations).setVisible(true);
	}

}