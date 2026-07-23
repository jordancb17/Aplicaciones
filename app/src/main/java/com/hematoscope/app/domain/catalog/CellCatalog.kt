package com.hematoscope.app.domain.catalog

import com.hematoscope.app.data.model.CellCategory
import com.hematoscope.app.data.model.CellType
import com.hematoscope.app.data.model.CountingGroup
import com.hematoscope.app.data.model.DescriptorAxis
import com.hematoscope.app.data.model.MorphologyDescriptor

/**
 * Reference data for peripheral-blood and bone-marrow cytomorphology.
 *
 * Values are typical adult ranges from standard haematology references
 * (Rodak's Hematology, Bain's *Blood Cells: A Practical Guide*). They are
 * intended as decision support for a trained observer, not as a substitute
 * for laboratory judgement.
 */
object CellCatalog {

    // ARGB accent colours reused across counter keypad and charts.
    private const val C_NEUTRO = 0xFF3F7FBF
    private const val C_LYMPH = 0xFF7B5EA7
    private const val C_MONO = 0xFF4CAF7D
    private const val C_EOS = 0xFFE0623C
    private const val C_BASO = 0xFF6D4C41
    private const val C_PRECURSOR = 0xFFB4123A
    private const val C_RBC = 0xFFC62828
    private const val C_PLT = 0xFFF9A825
    private const val C_OTHER = 0xFF607D8B

    /** All morphological cell types, ordered for display. */
    val cellTypes: List<CellType> = listOf(
        // ---------------------------------------------------------------- GRANULOCYTES
        CellType(
            id = "neutrophil_segmented",
            name = "Neutrófilo segmentado",
            synonyms = listOf("PMN", "polimorfonuclear", "segmented neutrophil"),
            category = CellCategory.GRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 12f..15f,
            ncRatio = "Bajo (abundante citoplasma)",
            keyFeatures = listOf(
                "Núcleo con 2–5 lóbulos unidos por finos puentes de cromatina",
                "Citoplasma rosa pálido con granulación neutra fina",
                "Referencia adulto: 40–70 % del diferencial"
            ),
            cytoplasm = "Rosa pálido, granulación específica fina y uniforme.",
            nucleus = "2–5 lóbulos, cromatina densa y grumosa.",
            clinicalNote = "Aumenta en infección bacteriana e inflamación (neutrofilia).",
            shortcut = "N",
            color = C_NEUTRO
        ),
        CellType(
            id = "neutrophil_band",
            name = "Neutrófilo en banda (cayado)",
            synonyms = listOf("band", "stab", "cayado", "bandemia"),
            category = CellCategory.GRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 12f..15f,
            ncRatio = "Bajo",
            keyFeatures = listOf(
                "Núcleo en herradura/banda sin segmentación completa",
                "El puente nuclear conserva ancho > 1/3 del lóbulo",
                "Referencia adulto: 0–5 %"
            ),
            cytoplasm = "Igual que el segmentado.",
            nucleus = "Banda curva sin constricciones filiformes.",
            clinicalNote = "Su aumento indica desviación a la izquierda (respuesta aguda).",
            shortcut = "B",
            color = C_NEUTRO
        ),
        CellType(
            id = "eosinophil",
            name = "Eosinófilo",
            synonyms = listOf("eosinophil"),
            category = CellCategory.GRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 12f..17f,
            ncRatio = "Bajo",
            keyFeatures = listOf(
                "Granulación gruesa naranja-rojiza refringente",
                "Núcleo habitualmente bilobulado ('gafas')",
                "Referencia adulto: 1–4 %"
            ),
            cytoplasm = "Granulación grande, uniforme, eosinófila brillante.",
            nucleus = "Bilobulado.",
            clinicalNote = "Aumenta en alergia, parasitosis y algunas neoplasias.",
            shortcut = "E",
            color = C_EOS
        ),
        CellType(
            id = "basophil",
            name = "Basófilo",
            synonyms = listOf("basophil"),
            category = CellCategory.GRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 10f..14f,
            ncRatio = "Medio",
            keyFeatures = listOf(
                "Granulación gruesa azul-violácea muy oscura",
                "Los gránulos suelen enmascarar el núcleo",
                "Referencia adulto: 0–1 %"
            ),
            cytoplasm = "Gránulos grandes basófilos, hidrosolubles.",
            nucleus = "Bilobulado, con frecuencia oculto por los gránulos.",
            clinicalNote = "Aumenta en síndromes mieloproliferativos (p. ej. LMC).",
            shortcut = "Ba",
            color = C_BASO
        ),

        // ------------------------------------------------------------- AGRANULOCYTES
        CellType(
            id = "lymphocyte",
            name = "Linfocito",
            synonyms = listOf("lymphocyte"),
            category = CellCategory.AGRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 7f..12f,
            ncRatio = "Alto (núcleo ocupa casi toda la célula)",
            keyFeatures = listOf(
                "Núcleo redondo con cromatina densa en bloques",
                "Escaso ribete de citoplasma azul cielo",
                "Referencia adulto: 20–40 %"
            ),
            cytoplasm = "Escaso, azul pálido, sin granulación (a veces algún gránulo azurófilo).",
            nucleus = "Redondo/ovalado, cromatina muy condensada.",
            clinicalNote = "Aumenta en infecciones virales y síndromes linfoproliferativos.",
            shortcut = "L",
            color = C_LYMPH
        ),
        CellType(
            id = "reactive_lymphocyte",
            name = "Linfocito reactivo (atípico)",
            synonyms = listOf("linfocito activado", "virocito", "Downey", "reactive lymphocyte"),
            category = CellCategory.AGRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 12f..25f,
            ncRatio = "Variable",
            keyFeatures = listOf(
                "Célula grande con citoplasma abundante y basófilo",
                "Borde citoplasmático que se moldea sobre los hematíes vecinos",
                "Cromatina más laxa que el linfocito maduro"
            ),
            cytoplasm = "Abundante, azul intenso, se indenta contra células vecinas.",
            nucleus = "Grande, puede mostrar nucléolo; cromatina reticular.",
            clinicalNote = "Característico de mononucleosis infecciosa y otras viriasis.",
            shortcut = "LR",
            color = C_LYMPH
        ),
        CellType(
            id = "monocyte",
            name = "Monocito",
            synonyms = listOf("monocyte"),
            category = CellCategory.AGRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 15f..20f,
            ncRatio = "Medio",
            keyFeatures = listOf(
                "Célula más grande de la sangre periférica",
                "Núcleo plegado/reniforme en herradura",
                "Citoplasma gris-azulado 'en vidrio esmerilado' con vacuolas"
            ),
            cytoplasm = "Gris azulado, aspecto esmerilado, vacuolas y finos gránulos.",
            nucleus = "Reniforme o plegado, cromatina laxa en peine.",
            clinicalNote = "Aumenta en infecciones crónicas, recuperación medular y SMD.",
            shortcut = "M",
            color = C_MONO
        ),
        CellType(
            id = "plasma_cell",
            name = "Célula plasmática",
            synonyms = listOf("plasmocito", "plasma cell"),
            category = CellCategory.AGRANULOCYTE,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 14f..20f,
            ncRatio = "Medio",
            keyFeatures = listOf(
                "Núcleo excéntrico con cromatina 'en rueda de carro'",
                "Citoplasma basófilo intenso con halo perinuclear (arquoplasma)",
                "Normal: rara vez en sangre periférica"
            ),
            cytoplasm = "Muy basófilo, zona clara perinuclear (aparato de Golgi).",
            nucleus = "Excéntrico, cromatina en radios de rueda.",
            clinicalNote = "Presencia notable en mieloma, infecciones y estados reactivos.",
            shortcut = "P",
            color = C_LYMPH
        ),

        // ---------------------------------------------------------------- PRECURSORS
        CellType(
            id = "blast",
            name = "Blasto",
            synonyms = listOf("mieloblasto", "linfoblasto", "blast"),
            category = CellCategory.PRECURSOR,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 14f..20f,
            ncRatio = "Muy alto",
            keyFeatures = listOf(
                "Cromatina fina y homogénea, 'inmadura'",
                "1–4 nucléolos visibles",
                "Escaso citoplasma; los bastones de Auer indican estirpe mieloide"
            ),
            cytoplasm = "Escaso, basófilo, sin o con escasa granulación.",
            nucleus = "Grande, cromatina delicada, nucléolos prominentes.",
            clinicalNote = "> 20 % en médula/sangre orienta a leucemia aguda.",
            shortcut = "Bl",
            color = C_PRECURSOR
        ),
        CellType(
            id = "promyelocyte",
            name = "Promielocito",
            synonyms = listOf("promyelocyte"),
            category = CellCategory.PRECURSOR,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 16f..25f,
            ncRatio = "Alto",
            keyFeatures = listOf(
                "Célula grande con granulación primaria (azurófila) prominente",
                "Zona de Golgi clara junto al núcleo",
                "Nucléolo aún visible"
            ),
            cytoplasm = "Basófilo con abundantes gránulos azurófilos gruesos.",
            nucleus = "Redondo/oval, cromatina algo más condensada que el blasto.",
            clinicalNote = "Su acumulación es típica de leucemia promielocítica aguda (LPA).",
            shortcut = "Pm",
            color = C_PRECURSOR
        ),
        CellType(
            id = "myelocyte",
            name = "Mielocito",
            synonyms = listOf("myelocyte"),
            category = CellCategory.PRECURSOR,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 12f..18f,
            ncRatio = "Medio",
            keyFeatures = listOf(
                "Aparece la granulación específica (secundaria)",
                "Núcleo redondo/oval sin indentación, sin nucléolo",
                "'Punto de amanecer': zona clara de Golgi"
            ),
            cytoplasm = "Granulación específica; menos basófilo.",
            nucleus = "Redondo u oval, cromatina condensándose.",
            clinicalNote = "Su presencia en sangre indica desviación a la izquierda / mieloproliferación.",
            shortcut = "My",
            color = C_PRECURSOR
        ),
        CellType(
            id = "metamyelocyte",
            name = "Metamielocito",
            synonyms = listOf("metamyelocyte", "juvenil"),
            category = CellCategory.PRECURSOR,
            countingGroup = CountingGroup.WBC_DIFFERENTIAL,
            sizeMicrons = 10f..15f,
            ncRatio = "Bajo-medio",
            keyFeatures = listOf(
                "Núcleo reniforme (indentado < 50 % del diámetro)",
                "Cromatina condensada, sin nucléolo",
                "Precede al neutrófilo en banda"
            ),
            cytoplasm = "Granulación específica, tono rosado.",
            nucleus = "Arriñonado, cromatina grumosa.",
            clinicalNote = "Componente de la desviación izquierda en infección/mieloproliferación.",
            shortcut = "Mm",
            color = C_PRECURSOR
        ),

        // ------------------------------------------------------------------ ERYTHROID
        CellType(
            id = "erythrocyte_normal",
            name = "Eritrocito normal (normocito)",
            synonyms = listOf("hematíe", "glóbulo rojo", "RBC"),
            category = CellCategory.ERYTHROID,
            countingGroup = CountingGroup.NOT_COUNTED,
            sizeMicrons = 6.5f..8.5f,
            ncRatio = "—",
            keyFeatures = listOf(
                "Disco bicóncavo anucleado",
                "Palidez central de ~1/3 del diámetro",
                "Diámetro similar al núcleo del linfocito pequeño (referencia de tamaño)"
            ),
            cytoplasm = "Rosa-anaranjado uniforme.",
            nucleus = "Ausente en el hematíe maduro.",
            clinicalNote = "Se evalúa por tamaño, color, forma, inclusiones y distribución.",
            shortcut = "H",
            color = C_RBC
        ),
        CellType(
            id = "polychromatophil",
            name = "Reticulocito / policromatófilo",
            synonyms = listOf("reticulocyte", "policromatofilia"),
            category = CellCategory.ERYTHROID,
            countingGroup = CountingGroup.NOT_COUNTED,
            sizeMicrons = 8f..10f,
            ncRatio = "—",
            keyFeatures = listOf(
                "Hematíe joven ligeramente mayor y azulado",
                "En Wright se ve gris-azulado (policromasia)",
                "Con azul de cresilo brillante muestra retículo azul"
            ),
            cytoplasm = "Azul-grisáceo por ARN residual.",
            nucleus = "Ausente.",
            clinicalNote = "Su aumento refleja respuesta medular regenerativa.",
            shortcut = "Rt",
            color = C_RBC
        ),
        CellType(
            id = "nrbc_normoblast",
            name = "Eritroblasto (normoblasto)",
            synonyms = listOf("NRBC", "eritrocito nucleado", "normoblast"),
            category = CellCategory.ERYTHROID,
            countingGroup = CountingGroup.NRBC_PER_100_WBC,
            sizeMicrons = 8f..12f,
            ncRatio = "Alto",
            keyFeatures = listOf(
                "Precursor rojo nucleado en sangre periférica",
                "Núcleo redondo excéntrico, cromatina en rueda",
                "Se informa por cada 100 leucocitos y corrige el recuento"
            ),
            cytoplasm = "Del azul (basófilo) al rosado según maduración.",
            nucleus = "Redondo, picnótico en estadios tardíos.",
            clinicalNote = "Indica estrés medular intenso, hemólisis o infiltración.",
            shortcut = "NE",
            color = C_RBC
        ),

        // ------------------------------------------------------------------ PLATELETS
        CellType(
            id = "platelet_normal",
            name = "Plaqueta",
            synonyms = listOf("trombocito", "platelet"),
            category = CellCategory.PLATELET,
            countingGroup = CountingGroup.NOT_COUNTED,
            sizeMicrons = 2f..4f,
            ncRatio = "—",
            keyFeatures = listOf(
                "Fragmento citoplasmático anucleado del megacariocito",
                "Granulación central púrpura (cromómero)",
                "Estimación: nº medio por campo 100× × 20.000 ≈ recuento/µL"
            ),
            cytoplasm = "Azul pálido con gránulos centrales.",
            nucleus = "Ausente.",
            clinicalNote = "Evaluar número, tamaño, agregados y granularidad.",
            shortcut = "Pq",
            color = C_PLT
        ),
        CellType(
            id = "platelet_giant",
            name = "Plaqueta gigante",
            synonyms = listOf("macrotrombocito", "giant platelet"),
            category = CellCategory.PLATELET,
            countingGroup = CountingGroup.NOT_COUNTED,
            sizeMicrons = 4f..10f,
            ncRatio = "—",
            keyFeatures = listOf(
                "Plaqueta del tamaño de un hematíe o mayor",
                "Sugiere recambio acelerado o trastorno congénito",
            ),
            cytoplasm = "Igual a la plaqueta normal, mayor superficie.",
            nucleus = "Ausente.",
            clinicalNote = "Frecuente en PTI, mieloproliferativos y síndromes Bernard-Soulier / MYH9.",
            shortcut = "PqG",
            color = C_PLT
        ),

        // --------------------------------------------------------------------- OTHER
        CellType(
            id = "smudge_cell",
            name = "Célula desnuda (sombra de Gumprecht)",
            synonyms = listOf("smudge cell", "manchas de Gumprecht"),
            category = CellCategory.OTHER,
            countingGroup = CountingGroup.NOT_COUNTED,
            keyFeatures = listOf(
                "Restos nucleares de células rotas al extender",
                "Abundantes de forma característica en LLC"
            ),
            cytoplasm = "Ausente / disgregado.",
            nucleus = "Cromatina difusa sin membrana definida.",
            clinicalNote = "Artefacto informativo: muy numerosas orientan a LLC.",
            shortcut = "S",
            color = C_OTHER
        ),
        CellType(
            id = "hairy_cell",
            name = "Tricoleucocito (célula peluda)",
            synonyms = listOf("hairy cell", "célula pilosa"),
            category = CellCategory.AGRANULOCYTE,
            countingGroup = CountingGroup.NOT_COUNTED,
            sizeMicrons = 10f..15f,
            ncRatio = "Medio-alto",
            keyFeatures = listOf(
                "Linfocito B neoplásico con proyecciones citoplasmáticas finas ('pelos')",
                "Núcleo oval o reniforme de cromatina laxa homogénea",
                "Positividad para TRAP; asociado a pancitopenia y esplenomegalia"
            ),
            cytoplasm = "Abundante, gris-azulado, con bordes deshilachados.",
            nucleus = "Oval/reniforme, sin nucléolo prominente.",
            clinicalNote = "Diagnóstico de la tricoleucemia (hairy cell leukemia).",
            shortcut = "HC",
            color = C_LYMPH
        ),
        CellType(
            id = "sezary_cell",
            name = "Célula de Sézary",
            synonyms = listOf("Sezary cell", "célula cerebriforme"),
            category = CellCategory.AGRANULOCYTE,
            countingGroup = CountingGroup.NOT_COUNTED,
            sizeMicrons = 10f..20f,
            ncRatio = "Alto",
            keyFeatures = listOf(
                "Linfocito T con núcleo cerebriforme (surcos profundos)",
                "Cromatina densa plegada 'en cerebro'",
                "Circula en el síndrome de Sézary / micosis fungoide"
            ),
            cytoplasm = "Escaso, basófilo.",
            nucleus = "Cerebriforme, muy plegado.",
            clinicalNote = "Marcador del linfoma cutáneo de células T (fase leucémica).",
            shortcut = "Sz",
            color = C_LYMPH
        )
    )

    /** Cell types offered as keys in the differential counter. */
    val countableCells: List<CellType>
        get() = cellTypes.filter { it.countingGroup != CountingGroup.NOT_COUNTED }

    fun cellById(id: String): CellType? = cellTypes.firstOrNull { it.id == id }

    // ============================================================ DESCRIPTORS

    /**
     * Semi-quantitative and qualitative morphological descriptors, grouped by axis.
     * Red-cell findings ([gradable] = true) are reported as 0 / 1+ / 2+ / 3+.
     */
    val descriptors: List<MorphologyDescriptor> = listOf(
        // ---- Red-cell size
        MorphologyDescriptor(
            id = "anisocytosis", name = "Anisocitosis",
            synonyms = listOf("anisocytosis"),
            axis = DescriptorAxis.SIZE, appliesTo = CellCategory.ERYTHROID,
            definition = "Variación anormal del tamaño de los hematíes (se correlaciona con el RDW).",
            significance = "Inespecífica; aparece en la mayoría de las anemias."
        ),
        MorphologyDescriptor(
            id = "microcytosis", name = "Microcitosis",
            synonyms = listOf("microcyte", "microcito"),
            axis = DescriptorAxis.SIZE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíes de diámetro < 6,5 µm (menores que el núcleo del linfocito pequeño).",
            significance = "Ferropenia, talasemia, anemia de enfermedad crónica."
        ),
        MorphologyDescriptor(
            id = "macrocytosis", name = "Macrocitosis",
            synonyms = listOf("macrocyte", "macrocito"),
            axis = DescriptorAxis.SIZE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíes de diámetro > 8,5 µm.",
            significance = "Megaloblástica (B12/folato), hepatopatía, alcohol, reticulocitosis."
        ),
        // ---- Red-cell colour
        MorphologyDescriptor(
            id = "hypochromia", name = "Hipocromía",
            synonyms = listOf("hypochromia"),
            axis = DescriptorAxis.COLOR, appliesTo = CellCategory.ERYTHROID,
            definition = "Aumento de la palidez central (> 1/3 del diámetro) por menor hemoglobina.",
            significance = "Ferropenia, talasemia, anemia sideroblástica."
        ),
        MorphologyDescriptor(
            id = "polychromasia", name = "Policromasia",
            synonyms = listOf("polychromasia", "policromatofilia"),
            axis = DescriptorAxis.COLOR, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíes azul-grisáceos (reticulocitos) por ARN residual.",
            significance = "Respuesta regenerativa: hemólisis o sangrado agudo."
        ),
        // ---- Red-cell shape (poikilocytosis)
        MorphologyDescriptor(
            id = "spherocyte", name = "Esferocito",
            synonyms = listOf("spherocyte"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíe esférico, denso, sin palidez central y de menor diámetro.",
            significance = "Esferocitosis hereditaria y anemia hemolítica autoinmune."
        ),
        MorphologyDescriptor(
            id = "target_cell", name = "Dianocito (codocito)",
            synonyms = listOf("target cell", "célula en diana"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Zona central de hemoglobina rodeada de halo pálido ('diana').",
            significance = "Talasemia, hepatopatía, hemoglobinopatías, postesplenectomía."
        ),
        MorphologyDescriptor(
            id = "schistocyte", name = "Esquistocito",
            synonyms = listOf("schistocyte", "fragmentocito", "helmet cell"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Fragmento de hematíe con bordes angulosos o en casco.",
            significance = "Hallazgo clave de microangiopatía (PTT/SHU, CID, válvulas)."
        ),
        MorphologyDescriptor(
            id = "sickle_cell", name = "Drepanocito",
            synonyms = listOf("sickle cell", "célula falciforme"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíe alargado en forma de hoz o media luna, con extremos puntiagudos.",
            significance = "Anemia de células falciformes (HbS)."
        ),
        MorphologyDescriptor(
            id = "teardrop", name = "Dacriocito",
            synonyms = listOf("teardrop", "dacrocyte", "célula en lágrima"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíe en forma de lágrima con un extremo elongado.",
            significance = "Mielofibrosis, metaplasia mieloide, talasemia."
        ),
        MorphologyDescriptor(
            id = "bite_cell", name = "Célula mordida (degmacito)",
            synonyms = listOf("bite cell", "degmacyte"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíe con una o más muescas semicirculares por retirada esplénica de cuerpos de Heinz.",
            significance = "Déficit de G6PD y otras hemólisis oxidativas."
        ),
        MorphologyDescriptor(
            id = "heinz_bodies", name = "Cuerpos de Heinz",
            synonyms = listOf("Heinz bodies"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Precipitados de hemoglobina desnaturalizada; solo visibles con tinción supravital (cristal violeta).",
            significance = "Estrés oxidativo: déficit de G6PD, hemoglobinas inestables.", gradable = false
        ),
        MorphologyDescriptor(
            id = "hbc_crystal", name = "Cristales de hemoglobina C",
            synonyms = listOf("HbC crystals", "cristal en barra"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Cristales intracelulares rectangulares densos que deforman el hematíe.",
            significance = "Enfermedad por hemoglobina C (HbCC) o HbSC.", gradable = false
        ),
        MorphologyDescriptor(
            id = "elliptocyte", name = "Eliptocito / ovalocito",
            synonyms = listOf("elliptocyte", "ovalocyte"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíe elíptico u ovalado.",
            significance = "Eliptocitosis hereditaria; ovalocitos en megaloblástica."
        ),
        MorphologyDescriptor(
            id = "acanthocyte", name = "Acantocito",
            synonyms = listOf("acanthocyte", "spur cell"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Espículas irregulares, de longitud y distribución desiguales.",
            significance = "Hepatopatía grave, abetalipoproteinemia, postesplenectomía."
        ),
        MorphologyDescriptor(
            id = "echinocyte", name = "Equinocito (crenado)",
            synonyms = listOf("echinocyte", "burr cell", "crenado"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Espículas cortas, regulares y uniformemente distribuidas.",
            significance = "Uremia, artefacto de secado; diferenciar del acantocito."
        ),
        MorphologyDescriptor(
            id = "stomatocyte", name = "Estomatocito",
            synonyms = listOf("stomatocyte"),
            axis = DescriptorAxis.SHAPE, appliesTo = CellCategory.ERYTHROID,
            definition = "Palidez central en forma de hendidura o boca.",
            significance = "Estomatocitosis hereditaria, hepatopatía, alcohol, artefacto."
        ),
        // ---- Red-cell inclusions
        MorphologyDescriptor(
            id = "howell_jolly", name = "Cuerpos de Howell-Jolly",
            synonyms = listOf("Howell-Jolly bodies"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Restos nucleares de ADN, redondos, únicos, púrpura intenso.",
            significance = "Asplenia/hipoesplenismo, megaloblástica.", gradable = false
        ),
        MorphologyDescriptor(
            id = "basophilic_stippling", name = "Punteado basófilo",
            synonyms = listOf("basophilic stippling", "punctata"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Múltiples gránulos azules finos por agregados de ARN.",
            significance = "Intoxicación por plomo, talasemia, diseritropoyesis."
        ),
        MorphologyDescriptor(
            id = "pappenheimer", name = "Cuerpos de Pappenheimer",
            synonyms = listOf("Pappenheimer bodies", "siderocitos"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Gránulos de hierro en la periferia; azul con Perls.",
            significance = "Anemia sideroblástica, postesplenectomía.", gradable = false
        ),
        MorphologyDescriptor(
            id = "cabot_rings", name = "Anillos de Cabot",
            synonyms = listOf("Cabot rings"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Estructura en anillo o en 8, resto del huso mitótico.",
            significance = "Diseritropoyesis, megaloblástica, saturnismo.", gradable = false
        ),
        MorphologyDescriptor(
            id = "malaria", name = "Parásitos intraeritrocitarios",
            synonyms = listOf("Plasmodium", "malaria", "Babesia"),
            axis = DescriptorAxis.INCLUSION, appliesTo = CellCategory.ERYTHROID,
            definition = "Formas en anillo, trofozoítos o gametocitos dentro del hematíe.",
            significance = "Malaria/babesiosis; requiere identificación de especie.", gradable = false
        ),
        // ---- Red-cell distribution
        MorphologyDescriptor(
            id = "rouleaux", name = "Formación en pilas (rouleaux)",
            synonyms = listOf("rouleaux", "apilamiento"),
            axis = DescriptorAxis.DISTRIBUTION, appliesTo = CellCategory.ERYTHROID,
            definition = "Hematíes apilados como monedas.",
            significance = "Hiperproteinemia: mieloma, inflamación (VSG alta)."
        ),
        MorphologyDescriptor(
            id = "agglutination", name = "Aglutinación eritrocitaria",
            synonyms = listOf("agglutination"),
            axis = DescriptorAxis.DISTRIBUTION, appliesTo = CellCategory.ERYTHROID,
            definition = "Grumos irregulares tridimensionales de hematíes.",
            significance = "Enfermedad por crioaglutininas.", gradable = false
        ),
        // ---- White-cell nucleus
        MorphologyDescriptor(
            id = "hypersegmentation", name = "Hipersegmentación neutrofílica",
            synonyms = listOf("hypersegmentation"),
            axis = DescriptorAxis.NUCLEUS, appliesTo = CellCategory.GRANULOCYTE,
            definition = "Neutrófilos con ≥ 6 lóbulos o ≥ 5 % con 5 lóbulos.",
            significance = "Anemia megaloblástica (B12/folato).", gradable = false
        ),
        MorphologyDescriptor(
            id = "pelger_huet", name = "Anomalía de Pelger-Huët",
            synonyms = listOf("Pelger-Huet", "pince-nez"),
            axis = DescriptorAxis.NUCLEUS, appliesTo = CellCategory.GRANULOCYTE,
            definition = "Núcleos bilobulados 'en gafas' o no segmentados con cromatina densa.",
            significance = "Congénita (benigna) o pseudo-Pelger adquirida (SMD).", gradable = false
        ),
        MorphologyDescriptor(
            id = "left_shift", name = "Desviación a la izquierda",
            synonyms = listOf("left shift"),
            axis = DescriptorAxis.NUCLEUS, appliesTo = CellCategory.GRANULOCYTE,
            definition = "Aumento de formas inmaduras (cayados, metamielocitos, mielocitos).",
            significance = "Infección/inflamación aguda o mieloproliferación.", gradable = false
        ),
        // ---- White-cell cytoplasm
        MorphologyDescriptor(
            id = "toxic_granulation", name = "Granulación tóxica",
            synonyms = listOf("toxic granulation"),
            axis = DescriptorAxis.CYTOPLASM, appliesTo = CellCategory.GRANULOCYTE,
            definition = "Gránulos azurófilos gruesos y oscuros en el neutrófilo.",
            significance = "Infección bacteriana grave, sepsis, tras G-CSF."
        ),
        MorphologyDescriptor(
            id = "dohle_bodies", name = "Cuerpos de Döhle",
            synonyms = listOf("Dohle bodies"),
            axis = DescriptorAxis.CYTOPLASM, appliesTo = CellCategory.GRANULOCYTE,
            definition = "Inclusiones azul pálido de ARN en la periferia citoplasmática.",
            significance = "Infección, inflamación; también anomalía de May-Hegglin.", gradable = false
        ),
        MorphologyDescriptor(
            id = "vacuolization", name = "Vacuolización citoplasmática",
            synonyms = listOf("vacuolization", "vacuolas"),
            axis = DescriptorAxis.CYTOPLASM, appliesTo = CellCategory.GRANULOCYTE,
            definition = "Vacuolas en el citoplasma del neutrófilo (fagocitosis activa).",
            significance = "Sepsis bacteriana (junto a granulación tóxica y Döhle)."
        ),
        MorphologyDescriptor(
            id = "auer_rods", name = "Bastones de Auer",
            synonyms = listOf("Auer rods"),
            axis = DescriptorAxis.CYTOPLASM, appliesTo = CellCategory.PRECURSOR,
            definition = "Agregados lineales rojizos de material azurófilo en el citoplasma.",
            significance = "Diagnósticos de estirpe mieloide (leucemia mieloide aguda).", gradable = false
        ),
        // ---- Platelets
        MorphologyDescriptor(
            id = "platelet_clumping", name = "Agregados plaquetarios",
            synonyms = listOf("platelet clumping", "satelitismo"),
            axis = DescriptorAxis.DISTRIBUTION, appliesTo = CellCategory.PLATELET,
            definition = "Cúmulos de plaquetas o satelitismo alrededor de neutrófilos.",
            significance = "Causa de pseudotrombocitopenia por EDTA; repetir con citrato.", gradable = false
        ),
        MorphologyDescriptor(
            id = "giant_platelets_desc", name = "Plaquetas gigantes",
            synonyms = listOf("macrothrombocytes"),
            axis = DescriptorAxis.SIZE, appliesTo = CellCategory.PLATELET,
            definition = "Plaquetas ≥ tamaño de un hematíe.",
            significance = "PTI, mieloproliferativos, trastornos congénitos."
        )
    )

    fun descriptorsFor(category: CellCategory): List<MorphologyDescriptor> =
        descriptors.filter { it.appliesTo == category }

    fun descriptorById(id: String): MorphologyDescriptor? =
        descriptors.firstOrNull { it.id == id }
}
