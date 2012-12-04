package com.cse5473.securegame;

/**
 * 
 * @author Caruso
 */
public class Coordinate {
	/**
	 * 
	 */
	private final int X, Y;
	
	/**
	 * 
	 * @param x
	 * @param y
	 */
	public Coordinate (int x, int y) {
		this.X = x;
		this.Y = y;
	}
	
	/**
	 * 
	 * @param coordinate
	 */
	public Coordinate (String coordinate) {
		this.X = Integer.parseInt(coordinate.substring(0, 0));
		this.Y = Integer.parseInt(coordinate.substring(2, 2));
	}
	
	/**
	 * 
	 * @return
	 */
	public final int getX() {
		return this.X;
	}
	
	/**
	 * 
	 * @return
	 */
	public final int getY() {
		return this.Y;
	}
	
	/**
	 * 
	 */
	public final boolean equals (Object o) {
		Coordinate cmpr = (Coordinate)o;
		return (cmpr.getX() == this.X && cmpr.getY() == this.Y);
	}
	
	/**
	 * 
	 */
	public final String toString() {
		return this.X + "," + this.Y;
	}
}
