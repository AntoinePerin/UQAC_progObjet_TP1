package com.uqac.tp1;

import java.io.Serializable;
import java.util.Arrays;

public class Commande implements Serializable {
	
	private String[] proprietes;
	
	public Commande(String[] proprietes) {
		this.proprietes=proprietes;
	}
	
	public Commande() {
		super();
	}

	public String[] getProprietes() {
		return proprietes;
	}

	public void setProprietes(String[] proprietes) {
		this.proprietes = proprietes;
	}

	@Override
	public String toString() {
		return "Commande proprietes=" + Arrays.toString(proprietes) ;
	}
	
}
