package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.ProstutiTheme
import com.example.ui.theme.ProstutiTypography

object MathRenderer {

    private val superscriptMap = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ', 'a' to 'ᵃ', 'b' to 'ᵇ', 't' to 'ᵗ'
    )

    private val subscriptMap = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'o' to 'ₒ', 'x' to 'ₓ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'p' to 'ₚ', 's' to 'ₛ', 't' to 'ₜ'
    )

    private val symbolReplacements = listOf(
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\epsilon" to "ε",
        "\\theta" to "θ",
        "\\lambda" to "λ",
        "\\mu" to "μ",
        "\\pi" to "π",
        "\\sigma" to "σ",
        "\\tau" to "τ",
        "\\phi" to "φ",
        "\\omega" to "ω",
        "\\Delta" to "Δ",
        "\\Gamma" to "Γ",
        "\\Theta" to "Θ",
        "\\Lambda" to "Λ",
        "\\Sigma" to "Σ",
        "\\Phi" to "Φ",
        "\\Omega" to "Ω",
        "\\infty" to "∞",
        "\\pm" to "±",
        "\\mp" to "∓",
        "\\leq" to "≤",
        "\\le" to "≤",
        "\\geq" to "≥",
        "\\ge" to "≥",
        "\\neq" to "≠",
        "\\ne" to "≠",
        "\\approx" to "≈",
        "\\times" to "×",
        "\\cdot" to "·",
        "\\div" to "÷",
        "\\sum" to "∑",
        "\\int" to "∫",
        "\\partial" to "∂",
        "\\nabla" to "∇",
        "\\to" to "→",
        "\\rightarrow" to "→",
        "\\leftarrow" to "←",
        "\\in" to "∈",
        "\\notin" to "∉",
        "\\forall" to "∀",
        "\\exists" to "∃",
        "\\cos" to "cos",
        "\\sin" to "sin",
        "\\tan" to "tan",
        "\\exp" to "exp",
        "\\log" to "log",
        "\\ln" to "ln"
    )

    fun render(input: String): String {
        var text = input

        // Replace $$...$$ with block math
        val blockRegex = Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL)
        text = blockRegex.replace(text) { match ->
            formatMathExpression(match.groupValues[1])
        }

        // Replace $...$ with inline math
        val inlineRegex = Regex("""\$(.*?)\$""")
        text = inlineRegex.replace(text) { match ->
            formatMathExpression(match.groupValues[1])
        }

        return text
    }

    private fun formatMathExpression(expr: String): String {
        var result = expr.trim()

        // Handle \frac{a}{b}
        val fracRegex = Regex("""\\frac\{(.*?)\}\{(.*?)\}""")
        result = fracRegex.replace(result) { match ->
            val num = match.groupValues[1].trim()
            val den = match.groupValues[2].trim()
            "($num)/($den)"
        }

        // Handle \sqrt{a} or \sqrt[n]{a}
        val sqrtRegex = Regex("""\\sqrt(\[(.*?)\])?\{(.*?)\}""")
        result = sqrtRegex.replace(result) { match ->
            val inner = match.groupValues[3].trim()
            val root = match.groupValues[2].trim()
            if (root.isNotEmpty()) "${toSuperscript(root)}√($inner)" else "√($inner)"
        }

        // Replace LaTeX symbols
        for ((latex, unicode) in symbolReplacements) {
            result = result.replace(latex, unicode)
        }

        // Handle superscripts: ^{abc} or ^x
        val groupSupRegex = Regex("""\^\{(.*?)\}""")
        result = groupSupRegex.replace(result) { match ->
            toSuperscript(match.groupValues[1])
        }
        val singleSupRegex = Regex("""\^([0-9a-zA-Z+-])""")
        result = singleSupRegex.replace(result) { match ->
            toSuperscript(match.groupValues[1])
        }

        // Handle subscripts: _{abc} or _x
        val groupSubRegex = Regex("""_\{(.*?)\}""")
        result = groupSubRegex.replace(result) { match ->
            toSubscript(match.groupValues[1])
        }
        val singleSubRegex = Regex("""_([0-9a-zA-Z+-])""")
        result = singleSubRegex.replace(result) { match ->
            toSubscript(match.groupValues[1])
        }

        // Remove lingering backslashes
        result = result.replace(Regex("""\\([a-zA-Z]+)""")) { match ->
            match.groupValues[1]
        }

        return result
    }

    fun toSuperscript(str: String): String {
        return str.map { superscriptMap[it] ?: it }.joinToString("")
    }

    fun toSubscript(str: String): String {
        return str.map { subscriptMap[it] ?: it }.joinToString("")
    }
}

@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ProstutiTypography.bodyLarge,
    color: Color = ProstutiTheme.colors.ink
) {
    val rendered = MathRenderer.render(text)
    Text(
        text = rendered,
        modifier = modifier,
        style = style,
        color = color
    )
}
