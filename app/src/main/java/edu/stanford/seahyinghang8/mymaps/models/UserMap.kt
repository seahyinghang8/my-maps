package edu.stanford.seahyinghang8.mymaps.models

import java.io.Serializable

data class UserMap(var title: String, var description: String, var places: List<Place>) : Serializable