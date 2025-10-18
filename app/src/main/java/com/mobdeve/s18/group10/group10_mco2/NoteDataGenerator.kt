package com.mobdeve.s18.group10.group10_mco2

class NoteDataGenerator {
    companion object {
        fun generateNotes(): ArrayList<Note> {
            var tempNotes = ArrayList<Note>()

            tempNotes.add(Note("Big ass spider on the bathroom...", "what the helly"))
            tempNotes.add(Note("Buy milk and eggs", "before they mysteriously disappear again"))
            tempNotes.add(Note("Android project deadline", "cry about it later"))
            tempNotes.add(Note("Gym motivation", "maybe tomorrow... or next week"))
            tempNotes.add(Note("Cat stole my charger", "again. little criminal."))
            tempNotes.add(Note("Reminder: clean laptop fan", "sounds like a jet engine now"))
            tempNotes.add(Note("Dream idea", "flying toaster saves the world"))
            tempNotes.add(Note("Mom called", "ask about that 'thing' she won’t text about"))
            tempNotes.add(Note("WiFi name ideas", "FBI Surveillance Van 3"))

            return tempNotes.shuffled() as ArrayList<Note>
        }
    }
}