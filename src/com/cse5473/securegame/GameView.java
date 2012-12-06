/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Please note, this is the one thing from the Apache Liscence that we used.
 * It had to be heavily edited from its original state (A one player game)
 * to be used within the context of a secure multiplayer game.
 */

package com.cse5473.securegame;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Handler.Callback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Gameview allows for the graphical display to function correctly within the
 * adroid framework.
 * 
 */
public class GameView extends View {
	/**
	 * Represents the speed of the blinking when a player is trying to decide
	 * where they will move
	 */
	public static final long FPS_MS = 1000 / 2;
	
	/**
	 * This is the enumeration for possible game states. These are marked in
	 * each square and are initially all set to State.EMPTY. The State.UNKOWN is
	 * ONLY used for initialization and error detection. If you ever see an
	 * UNKNOWN state while playing the game, then the code is broken.
	 */
	public enum State {
		UNKNOWN(-3), WIN(-2), EMPTY(0), PLAYER1(1), PLAYER2(2);
		
		/**
		 * The integer value assigned to each enumerator to distinguish.
		 */
		private int mValue;

		/**
		 * The sole constructor for a State, only takes in an Integer valuation.
		 * 
		 * @param value
		 *            The value representing the state.
		 */
		private State(int value) {
			mValue = value;
		}

		/**
		 * Gets the value assigned with this state.
		 * 
		 * @return The value assigned to the state.
		 */
		public int getValue() {
			return mValue;
		}

		/**
		 * Returns the state associated with the given value. Helps when
		 * converting back from JSONObject to Object.
		 * 
		 * @param i
		 *            The integer to evaluate the state from.
		 * @return The state associated with the supplied integer.
		 */
		public static State fromInt(int i) {
			for (State s : values()) {
				if (s.getValue() == i) {
					return s;
				}
			}
			return EMPTY;
		}
	}

	/**
	 * This sets the inital value for the margin surounding the game board to
	 * be, as default 4. This value can be changed and will likely be altered if
	 * the phone slips into horizontal view.
	 */
	private static final int MARGIN = 4;
	
	/**
	 * This is the integer value representing whether or not the game should be
	 * blinking the tiles when a user is about to finalize a move. (Default 1)
	 */
	private static final int MSG_BLINK = 1;

	/**
	 * This is the handler for the GameView.
	 */
	private final Handler mHandler = new Handler(new MyHandler());

	/**
	 * Used to store the width and height of the bitmaps associated with making
	 * a move.
	 */
	private final Rect mSrcRect = new Rect();
	
	/**
	 * Used to store the width and height of the places that the move can go.
	 */
	private final Rect mDstRect = new Rect();

	/**
	 * This is basically the size of the gameboard (Both for x and y as it is
	 * square)
	 */
	private int mSxy;
	
	/**
	 * The origianl offset in the x direction from the border surrounding the
	 * GameView. Determined upon construction, as phones will vary.
	 */
	private int mOffetX;
	
	/**
	 * The origianl offset in the y direction from the border surrounding the
	 * GameView. Determined upon construction, as phones will vary.
	 */
	private int mOffetY;
	
	/**
	 * Represents the winning paint graphics.
	 */
	private Paint mWinPaint;
	
	/**
	 * Also represents the winning paint graphics (In the form of the red line)
	 */
	private Paint mLinePaint;
	
	/**
	 * Represents the generic graphics for any BMP object.
	 */
	private Paint mBmpPaint;
	
	/**
	 * Represents the "x" character bitmap for player 1.
	 */
	private Bitmap mBmpPlayer1;
	
	/**
	 * Represents the "o" character bitmap for player 2.
	 */
	private Bitmap mBmpPlayer2;
	
	/**
	 * This represents the background object to be drawn for all game boards.
	 */
	private Drawable mDrawableBg;

	/**
	 * The cell listener for the gameview.
	 */
	private ICellListener mCellListener;

	/**
	 * Contains one of {@link State#EMPTY}, {@link State#PLAYER1} or
	 * {@link State#PLAYER2}.
	 */
	private final State[] mData = new State[9];

	/**
	 * The currently selected cell, initially -1, to be updated by selectCell.
	 */
	private int mSelectedCell = -1;
	
	/**
	 * The value of the selected cell, can also be obtained through
	 * mData[mSelectedCell].
	 */
	private State mSelectedValue = State.EMPTY;
	
	/**
	 * The current player, this is initialized to State.UNKNOWN. If the unkown
	 * state ever reaches the actualy game without being initialized by the
	 * GameActivity you recieve odd errors.
	 */
	private State mCurrentPlayer = State.UNKNOWN;
	
	/**
	 * The winner remains empty until a player wins.
	 */
	private State mWinner = State.EMPTY;

	/**
	 * The winning column (if any). Used for the winning paint.
	 */
	private int mWinCol = -1;
	
	/**
	 * The winning row (if any). Used for the winning paint.
	 */
	private int mWinRow = -1;
	
	/**
	 * The winning diagonal (if any). Used for the winning paint.
	 */
	private int mWinDiag = -1;

	/**
	 * Turns the blinking off when it is false.
	 */
	private boolean mBlinkDisplayOff;
	
	/**
	 * The size of the rectangle to blink (Draws a white rectangle).
	 */
	private final Rect mBlinkRect = new Rect();

	/**
	 * This is the abstract interface for the CellListener. This is how the
	 * application interacts with the game screen. It's listening for when a
	 * user selects a cell, then it adds a temporary marking. This is
	 * implemented later in the code.
	 */
	public interface ICellListener {
		abstract void onCellSelected();
	}

	/**
	 * The lone constructor for the type of 'GameView', it takes in a context
	 * and a set of attributes. This is basically the initialization of all
	 * graphical components of the application.
	 * 
	 * @param context
	 *            The context that will be passed to the extended View.
	 * @param attrs
	 *            The attributes to be passed to the extended View.
	 */
	@SuppressWarnings("deprecation")
	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		requestFocus();

		mDrawableBg = getResources().getDrawable(R.drawable.lib_bg);
		setBackgroundDrawable(mDrawableBg);

		mBmpPlayer1 = getResBitmap(R.drawable.lib_cross);
		mBmpPlayer2 = getResBitmap(R.drawable.lib_circle);

		if (mBmpPlayer1 != null) {
			mSrcRect.set(0, 0, mBmpPlayer1.getWidth() - 1,
					mBmpPlayer1.getHeight() - 1);
		}

		mBmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		mLinePaint = new Paint();
		mLinePaint.setColor(0xFFFFFFFF);
		mLinePaint.setStrokeWidth(5);
		mLinePaint.setStyle(Style.STROKE);

		mWinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mWinPaint.setColor(0xFFFF0000);
		mWinPaint.setStrokeWidth(10);
		mWinPaint.setStyle(Style.STROKE);

		for (int i = 0; i < mData.length; i++) {
			mData[i] = State.EMPTY;
		}

		if (isInEditMode()) {
			// In edit mode (e.g. in the Eclipse ADT graphical layout editor)
			// we'll use some random data to display the state.
			Random rnd = new Random();
			for (int i = 0; i < mData.length; i++) {
				mData[i] = State.fromInt(rnd.nextInt(3));
			}
		}
	}

	/**
	 * Returns the full array of all states that represent the board.
	 * 
	 * @return The State array representing the whole board.
	 */
	public State[] getData() {
		return mData;
	}

	/**
	 * Sets any cell in the board to be a state.
	 * 
	 * @param cellIndex
	 *            The index to set the state at.
	 * @param value
	 *            The value to be assigned to the index.
	 */
	public void setCell(int cellIndex, State value) {
		mData[cellIndex] = value;
		invalidate();
	}

	/**
	 * Sets the cell listener of the GameView to be whatever is listening, for
	 * instance this would most usefully be set in GameActivity for
	 * implementation with the Messages.
	 * 
	 * @param cellListener
	 *            The CellListener to tie to the GameView.
	 */
	public void setCellListener(ICellListener cellListener) {
		mCellListener = cellListener;
	}

	/**
	 * Gets the currently selected cell.. If no cell is selected (E.G. the
	 * player unselects a cell) then we return -1 to indicate that the player
	 * has not selected a cell.
	 * 
	 * @return The index of the selected cell.
	 */
	public int getSelection() {
		if (mSelectedValue == mCurrentPlayer) {
			return mSelectedCell;
		}

		return -1;
	}

	/**
	 * Returns whichever player should be moving currently, this is mainly used
	 * for selecting between X's and O's when placing a piece on the gameboard.
	 * 
	 * @return The state of the current player.
	 */
	public State getCurrentPlayer() {
		return mCurrentPlayer;
	}

	/**
	 * Used most commonly for turn switching, after the player makes his move we
	 * set the state to the opposite of whomever moved.
	 * 
	 * @param player
	 *            The player who is going to make a move next.
	 */
	public void setCurrentPlayer(State player) {
		mCurrentPlayer = player;
		mSelectedCell = -1;
	}

	/**
	 * Returns the winner if it has been set, if the game is not yet won the
	 * value will be State.UNKNOWN
	 * 
	 * @return The state representing which player has won the game.
	 */
	public State getWinner() {
		return mWinner;
	}

	/**
	 * Updates the winner field when a winner has been determined.
	 * 
	 * @param winner
	 *            WHomever has won the game.
	 */
	public void setWinner(State winner) {
		mWinner = winner;
	}

	/**
	 * Sets the value of which row, column or diagonal has won. For instance, if
	 * we get 3 in a row on the first column, then it would be col=1, the rest
	 * would be -1.
	 * 
	 * @param col
	 *            The victorious column, or -1 if it was a row or diagonal.
	 * @param row
	 *            The victorious row, or -1 if it was a column or diagonal.
	 * @param diagonal
	 *            The victorious diagonal, or -1 if it was a row or column.
	 */
	public void setFinished(int col, int row, int diagonal) {
		mWinCol = col;
		mWinRow = row;
		mWinDiag = diagonal;
	}

	/**
	 * This is the MAIN workhorse for drawing in GameView. It detects all
	 * boundaries and repaints the background with each new call to painting. If
	 * a user decides to change a cell this is the method that will update all
	 * graphical representations.
	 * 
	 * @param canvas
	 *            This is the canvas that is to be passed to the extended view.
	 */
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int sxy = mSxy;
		int s3 = sxy * 3;
		int x7 = mOffetX;
		int y7 = mOffetY;

		for (int i = 0, k = sxy; i < 2; i++, k += sxy) {
			canvas.drawLine(x7, y7 + k, x7 + s3 - 1, y7 + k, mLinePaint);
			canvas.drawLine(x7 + k, y7, x7 + k, y7 + s3 - 1, mLinePaint);
		}

		for (int j = 0, k = 0, y = y7; j < 3; j++, y += sxy) {
			for (int i = 0, x = x7; i < 3; i++, k++, x += sxy) {
				mDstRect.offsetTo(MARGIN + x, MARGIN + y);

				State v;
				if (mSelectedCell == k) {
					if (mBlinkDisplayOff) {
						continue;
					}
					v = mSelectedValue;
				} else {
					v = mData[k];
				}

				switch (v) {
				case PLAYER1:
					if (mBmpPlayer1 != null) {
						canvas.drawBitmap(mBmpPlayer1, mSrcRect, mDstRect,
								mBmpPaint);
					}
					break;
				case PLAYER2:
					if (mBmpPlayer2 != null) {
						canvas.drawBitmap(mBmpPlayer2, mSrcRect, mDstRect,
								mBmpPaint);
					}
					break;
				}
			}
		}

		if (mWinRow >= 0) {
			int y = y7 + mWinRow * sxy + sxy / 2;
			canvas.drawLine(x7 + MARGIN, y, x7 + s3 - 1 - MARGIN, y, mWinPaint);

		} else if (mWinCol >= 0) {
			int x = x7 + mWinCol * sxy + sxy / 2;
			canvas.drawLine(x, y7 + MARGIN, x, y7 + s3 - 1 - MARGIN, mWinPaint);

		} else if (mWinDiag == 0) {
			// diagonal 0 is from (0,0) to (2,2)

			canvas.drawLine(x7 + MARGIN, y7 + MARGIN, x7 + s3 - 1 - MARGIN, y7
					+ s3 - 1 - MARGIN, mWinPaint);

		} else if (mWinDiag == 1) {
			// diagonal 1 is from (0,2) to (2,0)

			canvas.drawLine(x7 + MARGIN, y7 + s3 - 1 - MARGIN, x7 + s3 - 1
					- MARGIN, y7 + MARGIN, mWinPaint);
		}
	}

	/**
	 * This is a helper method for ensuring that the gameboard will always be
	 * square, even when the phone is too small for the default size
	 * arrangements. For instance, when a phone is flipped sideways this method
	 * may be called to resize the board to fit on the screen.
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Keep the view squared
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
		setMeasuredDimension(d, d);
	}

	/**
	 * This is to be called, much like the onMeasure method, when the game board
	 * size MUST change. This is when someone unexpectedly flips their phone
	 * sideways...
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		int sx = (w - 2 * MARGIN) / 3;
		int sy = (h - 2 * MARGIN) / 3;

		int size = sx < sy ? sx : sy;

		mSxy = size;
		mOffetX = (w - 3 * size) / 2;
		mOffetY = (h - 3 * size) / 2;

		mDstRect.set(MARGIN, MARGIN, size - MARGIN, size - MARGIN);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();

		if (action == MotionEvent.ACTION_DOWN) {
			return true;

		} else if (action == MotionEvent.ACTION_UP) {
			int x = (int) event.getX();
			int y = (int) event.getY();

			int sxy = mSxy;
			x = (x - MARGIN) / sxy;
			y = (y - MARGIN) / sxy;

			if (isEnabled() && x >= 0 && x < 3 && y >= 0 & y < 3) {
				int cell = x + 3 * y;

				State state = cell == mSelectedCell ? mSelectedValue
						: mData[cell];
				state = state == State.EMPTY ? mCurrentPlayer : State.EMPTY;

				stopBlink();

				mSelectedCell = cell;
				mSelectedValue = state;
				mBlinkDisplayOff = false;
				mBlinkRect.set(MARGIN + x * sxy, MARGIN + y * sxy, MARGIN
						+ (x + 1) * sxy, MARGIN + (y + 1) * sxy);

				Log.d("FUCK","state is " + state);
				if (state != State.EMPTY) {
					// Start the blinker
					mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
				}

				if (mCellListener != null) {
					mCellListener.onCellSelected();
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Stops the blinking of the currently blinking cell. This is to be used
	 * when the move is to be finalized.
	 */
	public void stopBlink() {
		boolean hadSelection = mSelectedCell != -1
				&& mSelectedValue != State.EMPTY;
		mSelectedCell = -1;
		mSelectedValue = State.EMPTY;
		if (!mBlinkRect.isEmpty()) {
			invalidate(mBlinkRect);
		}
		mBlinkDisplayOff = false;
		mBlinkRect.setEmpty();
		mHandler.removeMessages(MSG_BLINK);
		if (hadSelection && mCellListener != null) {
			mCellListener.onCellSelected();
		}
	}

	/**
	 * Stores all MAIN components that make up a GameView in a Bundle, then
	 * turns this bundle into a Parcelable object for use elsewhere. This could
	 * easily be used for saving the game for a later state. This is used for
	 * pauses, resume and other really odd functions that are not typically
	 * expected during execution.
	 */
	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle b = new Bundle();

		Parcelable s = super.onSaveInstanceState();
		b.putParcelable("gv_super_state", s);

		b.putBoolean("gv_en", isEnabled());

		int[] data = new int[mData.length];
		for (int i = 0; i < data.length; i++) {
			data[i] = mData[i].getValue();
		}
		b.putIntArray("gv_data", data);

		b.putInt("gv_sel_cell", mSelectedCell);
		b.putInt("gv_sel_val", mSelectedValue.getValue());
		b.putInt("gv_curr_play", mCurrentPlayer.getValue());
		b.putInt("gv_winner", mWinner.getValue());

		b.putInt("gv_win_col", mWinCol);
		b.putInt("gv_win_row", mWinRow);
		b.putInt("gv_win_diag", mWinDiag);

		b.putBoolean("gv_blink_off", mBlinkDisplayOff);
		b.putParcelable("gv_blink_rect", mBlinkRect);

		return b;
	}

	/**
	 * This takes in a Parcelable state generated by onSaveInstanceState(). This
	 * method reads all values stored in the Parcelable state and assigns them
	 * to the GameView to restore the state that was previously placed inside
	 * the Bundle.
	 */
	@Override
	protected void onRestoreInstanceState(Parcelable state) {

		if (!(state instanceof Bundle)) {
			// Not supposed to happen.
			super.onRestoreInstanceState(state);
			return;
		}

		Bundle b = (Bundle) state;
		Parcelable superState = b.getParcelable("gv_super_state");

		setEnabled(b.getBoolean("gv_en", true));

		int[] data = b.getIntArray("gv_data");
		if (data != null && data.length == mData.length) {
			for (int i = 0; i < data.length; i++) {
				mData[i] = State.fromInt(data[i]);
			}
		}

		mSelectedCell = b.getInt("gv_sel_cell", -1);
		mSelectedValue = State.fromInt(b.getInt("gv_sel_val",
				State.EMPTY.getValue()));
		mCurrentPlayer = State.fromInt(b.getInt("gv_curr_play",
				State.EMPTY.getValue()));
		mWinner = State.fromInt(b.getInt("gv_winner", State.EMPTY.getValue()));

		mWinCol = b.getInt("gv_win_col", -1);
		mWinRow = b.getInt("gv_win_row", -1);
		mWinDiag = b.getInt("gv_win_diag", -1);

		mBlinkDisplayOff = b.getBoolean("gv_blink_off", false);
		Rect r = b.getParcelable("gv_blink_rect");
		if (r != null) {
			mBlinkRect.set(r);
		}

		// let the blink handler decide if it should blink or not
		mHandler.sendEmptyMessage(MSG_BLINK);

		super.onRestoreInstanceState(superState);
	}

	/**
	 * ?
	 */
	private class MyHandler implements Callback {
		public boolean handleMessage(Message msg) {
			if (msg.what == MSG_BLINK) {
				if (mSelectedCell >= 0 && mSelectedValue != State.EMPTY
						&& mBlinkRect.top != 0) {
					mBlinkDisplayOff = !mBlinkDisplayOff;
					invalidate(mBlinkRect);

					if (!mHandler.hasMessages(MSG_BLINK)) {
						mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
					}
				}
				return true;
			}
			return false;
		}
	}

	/**
	 * This bit of code allows for the resources from any bitmap stored in the
	 * assets to be drawn directly onto the board at the provided index.
	 * 
	 * @param bmpResId
	 *            The index to draw the bitmap at.
	 * @return The bitmap associated.
	 */
	private Bitmap getResBitmap(int bmpResId) {
		Options opts = new Options();
		opts.inDither = false;

		Resources res = getResources();
		Bitmap bmp = BitmapFactory.decodeResource(res, bmpResId, opts);

		if (bmp == null && isInEditMode()) {
			// BitmapFactory.decodeResource doesn't work from the rendering
			// library in Eclipse's Graphical Layout Editor. Use this workaround
			// instead.

			Drawable d = res.getDrawable(bmpResId);
			int w = d.getIntrinsicWidth();
			int h = d.getIntrinsicHeight();
			bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
			Canvas c = new Canvas(bmp);
			d.setBounds(0, 0, w - 1, h - 1);
			d.draw(c);
		}

		return bmp;
	}
}
