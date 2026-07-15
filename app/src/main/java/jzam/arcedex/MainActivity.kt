package jzam.arcedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import jzam.arcedex.ui.screens.ArcedexApp
import jzam.arcedex.ui.theme.ArcedexTheme
import jzam.arcedex.viewmodels.PokeResearchViewModel
import jzam.arcedex.viewmodels.PokeResearchViewModelFactory

/*
 * Arcedex by jzam (https://github.com/jzam)
 *
 * Main entry point for Arcedex.
 */
class MainActivity : ComponentActivity() {

    private val pokeResearchViewModel: PokeResearchViewModel by viewModels {
        PokeResearchViewModelFactory((application as ArcedexApplication).pokeResearchRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArcedexTheme {
                ArcedexApp(pokeResearchViewModel)
            }
        }
    }
}
