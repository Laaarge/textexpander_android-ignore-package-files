package com.dacotech.textexpanderapp

object TriggerRepository {
    val triggers = mutableListOf<Match>()

    data class Match(val trigger: String, val replace: String, val word: Boolean)

    fun loadTriggersFromYAML(yamlContent: String) {
        val yaml = org.yaml.snakeyaml.Yaml()
        val data = yaml.load<Map<String, Any>>(yamlContent)
        val matches = data["matches"] as List<Map<String, Any>>
        matches.forEach { match ->
            val replaceText = match["replace"] as? String ?: ""
            val word = match["word"] as? Boolean ?: false
            if ("triggers" in match) {
                (match["triggers"] as List<String>).forEach { trigger ->
                    triggers.add(Match(trigger, replaceText, word))
                }
            } else if ("trigger" in match) {
                triggers.add(Match(match["trigger"] as String, replaceText, word))
            }
        }
    }
}
