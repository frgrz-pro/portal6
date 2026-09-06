// Généré par apps/web/build_manifest.py — ne pas éditer à la main.
window.PORTAL6 = {
  "generated": "2026-09-06 20:20",
  "music": {
    "vault": [
      {
        "name": "extract_spotify.xlsx",
        "desc": "Export du Sheet « extract spotify » : 14 017 titres, 110 playlists",
        "kind": "source",
        "present": true,
        "path": "data/music/extract_spotify.xlsx",
        "size": 2553581,
        "size_h": "2.4 Mo",
        "mtime": "2026-08-19 22:31"
      },
      {
        "name": "library_scan.csv",
        "desc": "Scan local courant — périmètre `m:/music` uniquement",
        "kind": "source",
        "present": true,
        "path": "data/music/library_scan.csv",
        "size": 20646968,
        "size_h": "19.7 Mo",
        "mtime": "2026-08-20 23:36",
        "lines": 85040
      },
      {
        "name": "library_scan.2026-08-19.pre-quarantaine.csv",
        "desc": "Archive : scan initial, périmètre `m:/` complet",
        "kind": "archive",
        "present": true,
        "path": "data/music/library_scan.2026-08-19.pre-quarantaine.csv",
        "size": 21471317,
        "size_h": "20.5 Mo",
        "mtime": "2026-08-19 23:13",
        "lines": 88223
      },
      {
        "name": "local_duplicates_report.2026-08-20-0028.csv",
        "desc": "Rapport de dédup du run qui a servi à la quarantaine",
        "kind": "archive",
        "present": true,
        "path": "data/music/local_duplicates_report.2026-08-20-0028.csv",
        "size": 784343,
        "size_h": "766.0 Ko",
        "mtime": "2026-08-20 00:28",
        "lines": 4513
      },
      {
        "name": "quarantine_paths.2026-08-20-0028.txt",
        "desc": "Liste des fichiers déplacés vers `_a_trier`",
        "kind": "archive",
        "present": true,
        "path": "data/music/quarantine_paths.2026-08-20-0028.txt",
        "size": 254876,
        "size_h": "248.9 Ko",
        "mtime": "2026-08-20 00:28",
        "lines": 2464
      },
      {
        "name": "local_duplicates_report.csv",
        "desc": "Rapport de dédup du rescan (vide = plus de doublons)",
        "kind": "dérivé",
        "present": true,
        "path": "data/music/local_duplicates_report.csv",
        "size": 77,
        "size_h": "77 o",
        "mtime": "2026-08-20 23:37",
        "lines": 0
      },
      {
        "name": "quarantine_paths.txt",
        "desc": "Liste de quarantaine du rescan (vide)",
        "kind": "dérivé",
        "present": true,
        "path": "data/music/quarantine_paths.txt",
        "size": 3,
        "size_h": "3 o",
        "mtime": "2026-08-20 23:37",
        "lines": 0
      },
      {
        "name": "quarantine_duplicates.ps1",
        "desc": "Script PowerShell de déplacement généré par la dédup",
        "kind": "dérivé",
        "present": true,
        "path": "data/music/quarantine_duplicates.ps1",
        "size": 2899,
        "size_h": "2.8 Ko",
        "mtime": "2026-08-20 23:37"
      },
      {
        "name": "retag_plan.csv",
        "desc": "Plan de retag issu du fingerprint (run interrompu)",
        "kind": "dérivé",
        "present": true,
        "path": "data/music/retag_plan.csv",
        "size": 3146,
        "size_h": "3.1 Ko",
        "mtime": "2026-08-20 21:45",
        "lines": 20
      },
      {
        "name": ".fingerprint_cache.json",
        "desc": "Cache AcoustID du fingerprint",
        "kind": "cache",
        "present": true,
        "path": "data/music/.fingerprint_cache.json",
        "size": 479701,
        "size_h": "468.5 Ko",
        "mtime": "2026-08-20 22:26"
      },
      {
        "name": "fingerprint.log",
        "desc": "Journal du run de fingerprint",
        "kind": "cache",
        "present": true,
        "path": "data/music/fingerprint.log",
        "size": 3723,
        "size_h": "3.6 Ko",
        "mtime": "2026-08-20 22:26"
      }
    ],
    "db": {
      "path": "plugin/db/music.db",
      "size": 61202432,
      "size_h": "58.4 Mo",
      "mtime": "2026-09-06 18:58",
      "tables": {
        "enrichment": 13917,
        "files": 85040,
        "platform_refs": 0,
        "playlist_tracks": 13968,
        "playlists": 110,
        "tracks": 90080
      },
      "cross": {
        "both": 4408,
        "spotify_only": 9509,
        "local_only": 76163
      },
      "enrichment": [
        [
          "genres",
          13188
        ],
        [
          "country",
          11662
        ],
        [
          "mood",
          2933
        ],
        [
          "energy",
          2462
        ],
        [
          "valence",
          2239
        ],
        [
          "danceability",
          2940
        ],
        [
          "tempo",
          3163
        ],
        [
          "acousticness",
          2933
        ],
        [
          "instrumentalness",
          2933
        ],
        [
          "camelot",
          2772
        ]
      ],
      "enrichment_total": 13917,
      "top_playlists": [
        [
          "Discover 2024",
          7106
        ],
        [
          "Baltimore",
          421
        ],
        [
          "My Playlist #155",
          357
        ],
        [
          "Rockers Serenade",
          314
        ],
        [
          "Nostalgie Urbaine",
          292
        ],
        [
          "Klub der Visionare (R)",
          272
        ],
        [
          "Orishas",
          255
        ],
        [
          "Shaolin Temple",
          215
        ],
        [
          "Reading",
          211
        ],
        [
          "Rainforest Rhaspody",
          201
        ]
      ],
      "playlists": [
        {
          "id": "P6-PLS-0077",
          "name": "2013  (All)",
          "source": "spotify",
          "theme": "Embryon d'archive 2013 : blues touareg",
          "genres": "african, folk, niger, tuareg, desert blues",
          "artists": "Bombino, Idir, Afous d'Afous, Alhousseini Anivolla",
          "energy": null,
          "valence": null,
          "spotify": 1,
          "local": 1
        },
        {
          "id": "P6-PLS-0040",
          "name": "2014 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2014, dominante électro-world",
          "genres": "electronic, world, african, africa, hip-hop",
          "artists": "Fakear, JIM, Al'Tarba, Desmond Cheese",
          "energy": 0.64,
          "valence": 0.82,
          "spotify": 29,
          "local": 24
        },
        {
          "id": "P6-PLS-0016",
          "name": "2015 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2015, dominante world et blues malien",
          "genres": "electronic, world, african, blues, mali",
          "artists": "Terakaft, DJ Vadim, Ry Cooder, Tamikrest",
          "energy": 0.6,
          "valence": 0.67,
          "spotify": 48,
          "local": 32
        },
        {
          "id": "P6-PLS-0045",
          "name": "2016 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2016, dominante afro-funk",
          "genres": "funk, electronic, african, world, soul",
          "artists": "Fatoumata Diawara, Ali Farka Touré, Ann Peebles, Dub Dynasty",
          "energy": 0.79,
          "valence": 0.73,
          "spotify": 45,
          "local": 32
        },
        {
          "id": "P6-PLS-0030",
          "name": "2017 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2017, dominante latin et reggae",
          "genres": "electronic, latin, jazz, reggae, world",
          "artists": "Novalima, Gilles Peterson's Havana Cultura Band, Chancha Via Circuito, Wailing Souls",
          "energy": 0.63,
          "valence": 0.66,
          "spotify": 56,
          "local": 38
        },
        {
          "id": "P6-PLS-0021",
          "name": "2018 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2018, dominante folktronica andine",
          "genres": "electronic, house, funk, world, brazil",
          "artists": "Nicola Cruz, El Búho, Moderator, Joutro Mundo",
          "energy": 0.65,
          "valence": 0.63,
          "spotify": 88,
          "local": 68
        },
        {
          "id": "P6-PLS-0044",
          "name": "2019 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2019, dominante latin-jazz-funk très solaire",
          "genres": "electronic, latin, funk, jazz, world",
          "artists": "Montoya, Voilaaa, Hugh Masekela, El Búho",
          "energy": 0.69,
          "valence": 0.83,
          "spotify": 32,
          "local": 24
        },
        {
          "id": "P6-PLS-0009",
          "name": "2020 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2020, dominante Brésil et électro",
          "genres": "electronic, brazilian, brazil, funk, brasil",
          "artists": "Dame, Montoya, Kazy Lambist, Isaac Delusion",
          "energy": 0.66,
          "valence": 0.68,
          "spotify": 52,
          "local": 31
        },
        {
          "id": "P6-PLS-0069",
          "name": "2021 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2021, dominante melodic house",
          "genres": "electronic, house, deep house, techno, indie",
          "artists": "Ben Böhmer, Angelique Kidjo, Polo & Pan, Danitsa",
          "energy": 0.67,
          "valence": 0.48,
          "spotify": 35,
          "local": 25
        },
        {
          "id": "P6-PLS-0011",
          "name": "2022 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2022, dominante latin-pop et house",
          "genres": "electronic, latin, pop, house, hip-hop",
          "artists": "Bad Bunny, Yeahman, Jul, Danit",
          "energy": 0.59,
          "valence": 0.59,
          "spotify": 57,
          "local": 34
        },
        {
          "id": "P6-PLS-0028",
          "name": "2023 (All)",
          "source": "spotify",
          "theme": "Archive annuelle 2023, dominante rap east coast et électro",
          "genres": "hip-hop, rap, electronic, hip hop, east coast rap",
          "artists": "Fred again.., Manu Chao, Wu-Tang Clan, RZA",
          "energy": 0.6,
          "valence": 0.6,
          "spotify": 168,
          "local": 100
        },
        {
          "id": "P6-PLS-0001",
          "name": "2024 (All)",
          "source": "spotify",
          "theme": "Archive annuelle complète 2024, dominante rap/latin",
          "genres": "electronic, hip-hop, rap, reggae, latin",
          "artists": "Manu Chao, Wyclef Jean, Lithe, Nicola Cruz",
          "energy": 0.62,
          "valence": 0.69,
          "spotify": 143,
          "local": 82
        },
        {
          "id": "P6-PLS-0094",
          "name": "2024 (Best of)",
          "source": "spotify",
          "theme": "Best of annuel : rap et pop urbaine",
          "genres": "rap, hip-hop, hip hop, electronic, pop",
          "artists": "Baby Gang, Maes, Mala Rodríguez, Khaled",
          "energy": 0.57,
          "valence": 0.73,
          "spotify": 10,
          "local": 5
        },
        {
          "id": "P6-PLS-0051",
          "name": "_Hip-Hop",
          "source": "spotify",
          "theme": "Hip-hop abstract/alternatif (préfixe _ : en chantier ?)",
          "genres": "hip-hop, electronic, trip-hop, rap, hip hop",
          "artists": "Curse Ov Dialect, DJ Vadim, Roots Manuva, Promoe",
          "energy": 0.83,
          "valence": 0.93,
          "spotify": 63,
          "local": 33
        },
        {
          "id": "P6-PLS-0078",
          "name": "_Novalima, Gilles Peterson",
          "source": "spotify",
          "theme": "Graine de playlist latin-lounge autour de Novalima",
          "genres": "latin, electronic, lounge, peru, global fusion",
          "artists": "Novalima, The Bahama Soul Club, Arema Arega, A. Secret",
          "energy": null,
          "valence": null,
          "spotify": 7,
          "local": 2
        },
        {
          "id": "P6-PLS-0084",
          "name": "_Reggae Dancehall",
          "source": "spotify",
          "theme": "Embryon reggae/dancehall (2 titres)",
          "genres": "reggae, rap, dub, hip hop, hip-hop",
          "artists": "Promoe, Biga*Ranx",
          "energy": null,
          "valence": null,
          "spotify": 2,
          "local": 2
        },
        {
          "id": "P6-PLS-0106",
          "name": "_Rock Argentin",
          "source": "spotify",
          "theme": "Embryon rock/folk argentin",
          "genres": "latin, argentina, folk, indie es espanol, indie",
          "artists": "Onda Vaga, Jorge Drexler, Maldita Nerea, Soda Stereo",
          "energy": null,
          "valence": null,
          "spotify": 4,
          "local": 3
        },
        {
          "id": "P6-PLS-0034",
          "name": "Afro Fusion (R)",
          "source": "spotify",
          "theme": "Jazz et traditions d'Afrique, du highlife à la kora",
          "genres": "african, world, africa, jazz, mali",
          "artists": "the Garifuna Collective, Toumani Diabaté, Orchestra Baobab, Fela Kuti",
          "energy": 0.73,
          "valence": 0.82,
          "spotify": 68,
          "local": 49
        },
        {
          "id": "P6-PLS-0072",
          "name": "Analog Tones",
          "source": "spotify",
          "theme": "IDM et électronica expérimentale cérébrale (Aphex Twin, Four Tet)",
          "genres": "electronic, electronica, experimental, ambient, idm",
          "artists": "Aphex Twin, Four Tet, Amon Tobin, Caribou",
          "energy": 0.58,
          "valence": 0.21,
          "spotify": 61,
          "local": 47
        },
        {
          "id": "P6-PLS-0027",
          "name": "Anatol",
          "source": "spotify",
          "theme": "Rock psychédélique anatolien et Méditerranée orientale",
          "genres": "turkish, anatolian rock, psychedelic rock, world, folk",
          "artists": "Alsarah & The Nubatones, Altin Gün, Bakaka Band, Erkin Koray",
          "energy": null,
          "valence": null,
          "spotify": 20,
          "local": 14
        },
        {
          "id": "P6-PLS-0010",
          "name": "Arha",
          "source": "spotify",
          "theme": "Rap marseillais et club (Jul, SCH, Naps)",
          "genres": "rap, hip-hop, french rap, marseille, chicha",
          "artists": "Jul, SCH, Naps, Niska",
          "energy": 0.82,
          "valence": 0.55,
          "spotify": 39,
          "local": 27
        },
        {
          "id": "P6-PLS-0004",
          "name": "Backup",
          "source": "spotify",
          "theme": "Réservoir de secours : groove global éclectique",
          "genres": "hip-hop, rap, electronic, funk, hip hop",
          "artists": "BaianaSystem, Guts, Gyedu-Blay Ambolley, Brigitte Bardot",
          "energy": 0.76,
          "valence": 0.78,
          "spotify": 117,
          "local": 73
        },
        {
          "id": "P6-PLS-0006",
          "name": "Baltimore",
          "source": "spotify",
          "theme": "Soul/funk 60s-70s, l'Amérique de Marvin Gaye et James Brown",
          "genres": "soul, funk, rhythm and blues, rnb, 70s",
          "artists": "Marvin Gaye, James Brown, Funkadelic, Sharon Jones & The Dap-Kings",
          "energy": 0.61,
          "valence": 0.53,
          "spotify": 421,
          "local": 197
        },
        {
          "id": "P6-PLS-0062",
          "name": "Baobab",
          "source": "spotify",
          "theme": "Grandes voix d'Afrique de l'Ouest, Mali en tête",
          "genres": "african, africa, world, mali, world music",
          "artists": "Rokia Traoré, Oumou Sangaré, Danyèl Waro, Kandia Kouyate",
          "energy": null,
          "valence": null,
          "spotify": 17,
          "local": 11
        },
        {
          "id": "P6-PLS-0073",
          "name": "Best-of 2026",
          "source": "spotify",
          "theme": "Sélection de l'année en cours : disco-house funky et solaire",
          "genres": "electronic, funk, disco, house, soul",
          "artists": "Folamour, Christophe Laurent, João Selva, Clea Vincent",
          "energy": 0.7,
          "valence": 0.8,
          "spotify": 17,
          "local": 1
        },
        {
          "id": "P6-PLS-0050",
          "name": "Blue Moon",
          "source": "spotify",
          "theme": "Jazz classique et swing manouche, ambiance feutrée",
          "genres": "jazz, funk, chillout, jazz fusion, instrumental",
          "artists": "Django Reinhardt, Chet Baker, Quintette du Hot Club de France, Charlie Parker",
          "energy": null,
          "valence": null,
          "spotify": 69,
          "local": 26
        },
        {
          "id": "P6-PLS-0105",
          "name": "Blues",
          "source": "spotify",
          "theme": "Blues-rock guitare, du Delta au désert",
          "genres": "blues, guitar, rock, singer-songwriter, classic rock",
          "artists": "Ry Cooder, Jefferson Airplane, Ali Farka Touré, J.J. Cale",
          "energy": 0.94,
          "valence": 0.86,
          "spotify": 6,
          "local": 4
        },
        {
          "id": "P6-PLS-0043",
          "name": "Boogie Nights",
          "source": "spotify",
          "theme": "Disco et boogie pur sucre, euphorie 70s-80s (E0.83/V0.95 !)",
          "genres": "soul, disco, funk, 70s, rnb",
          "artists": "CHIC, Diana Ross, Patrice Rushen, The O'Jays",
          "energy": 0.83,
          "valence": 0.95,
          "spotify": 47,
          "local": 17
        },
        {
          "id": "P6-PLS-0100",
          "name": "Boom",
          "source": "spotify",
          "theme": "Micro-graine psytrance/tribal (4 titres)",
          "genres": "nan",
          "artists": "Hilight Tribe, Gastraxx, Decibel, Shpongle",
          "energy": null,
          "valence": null,
          "spotify": 3,
          "local": 2
        },
        {
          "id": "P6-PLS-0076",
          "name": "Brighton Beach",
          "source": "spotify",
          "theme": "Big beat UK énergie max (E0.98 — Fatboy Slim, Prodigy)",
          "genres": "electronic, electronica, house, dance, big beat",
          "artists": "Fatboy Slim, Asian Dub Foundation, Dub Pistols, The Prodigy",
          "energy": 0.98,
          "valence": 0.28,
          "spotify": 26,
          "local": 18
        },
        {
          "id": "P6-PLS-0065",
          "name": "Cafe Racer",
          "source": "spotify",
          "theme": "Surf rock et twang instrumental",
          "genres": "surf, surf rock, rock, instrumental, croatian",
          "artists": "Elvis Presley, The Drifters, Dick Dale, The Ventures",
          "energy": null,
          "valence": null,
          "spotify": 33,
          "local": 23
        },
        {
          "id": "P6-PLS-0018",
          "name": "Cat Walk",
          "source": "spotify",
          "theme": "Funk/soul à cuivres, groove félin et sûr de lui",
          "genres": "funk, soul, jazz, rhythm and blues, rnb",
          "artists": "James Brown, Cymande, The Souljazz Orchestra, Funkadelic",
          "energy": 0.79,
          "valence": 0.84,
          "spotify": 47,
          "local": 30
        },
        {
          "id": "P6-PLS-0041",
          "name": "Collectivo",
          "source": "spotify",
          "theme": "Cumbia, salsa et jazz colombien grand orchestre",
          "genres": "latin, jazz, cumbia, colombia, salsa",
          "artists": "Ondatrópica, Totó La Momposina, The Souljazz Orchestra, Chico Mann",
          "energy": 0.67,
          "valence": 0.77,
          "spotify": 130,
          "local": 80
        },
        {
          "id": "P6-PLS-0047",
          "name": "Coquillage",
          "source": "spotify",
          "theme": "Pop électro française rêveuse, plage et douce mélancolie",
          "genres": "electronic, indie pop, indie, pop, indietronica",
          "artists": "Polo & Pan, Bon Entendeur, Hypnolove, Emma Peters",
          "energy": 0.43,
          "valence": 0.33,
          "spotify": 38,
          "local": 26
        },
        {
          "id": "P6-PLS-0031",
          "name": "Creodelic",
          "source": "spotify",
          "theme": "Afro-funk créole et sono mondiale dansante",
          "genres": "funk, african, world, soul, lounge",
          "artists": "Pat Kalla, Voilaaa, Le Super Mojo, David Walters",
          "energy": 0.69,
          "valence": 0.81,
          "spotify": 161,
          "local": 50
        },
        {
          "id": "P6-PLS-0003",
          "name": "Discover 2024",
          "source": "spotify",
          "theme": "Méga-inbox de découvertes (7 697 titres) : matière première, pas une playlist d'écoute",
          "genres": "electronic, hip-hop, rap, soul, reggae",
          "artists": "COLORS, Manu Chao, Guts, Quantic",
          "energy": 0.67,
          "valence": 0.73,
          "spotify": 7106,
          "local": 670
        },
        {
          "id": "P6-PLS-0091",
          "name": "Downtempo",
          "source": "spotify",
          "theme": "Le genre au sens strict : nu-jazz et beats lents (E0.17)",
          "genres": "electronic, downtempo, trip-hop, chillout, hip-hop",
          "artists": "DJ Vadim, Nujabes, LTJ Bukem, Quantic",
          "energy": 0.17,
          "valence": 0.27,
          "spotify": 26,
          "local": 18
        },
        {
          "id": "P6-PLS-0037",
          "name": "Driftwood Dreams",
          "source": "spotify",
          "theme": "Folk songwriter, bois flotté et guitares douces",
          "genres": "folk, singer-songwriter, indie, freak folk, blues",
          "artists": "Devendra Banhart, Ry Cooder, CocoRosie, Bob Dylan",
          "energy": 0.49,
          "valence": 0.73,
          "spotify": 63,
          "local": 52
        },
        {
          "id": "P6-PLS-0070",
          "name": "Dub Odyssey",
          "source": "spotify",
          "theme": "Dub UK steppers profond et méditatif (V0.06 : le plus sombre du lot)",
          "genres": "reggae, dub, electronic, downtempo, chillout",
          "artists": "The Bush Chemists, Vibration Lab, Dub Dynasty, Alpha Steppa",
          "energy": 0.27,
          "valence": 0.06,
          "spotify": 62,
          "local": 40
        },
        {
          "id": "P6-PLS-0058",
          "name": "Dusty Road",
          "source": "spotify",
          "theme": "Pop/rock/R&B nord-américain hétéroclite, esprit road-trip",
          "genres": "rnb, canadian, pop, hip-hop, rap",
          "artists": "Drake, R.E.M., Alanis Morissette, The Weeknd",
          "energy": null,
          "valence": null,
          "spotify": 30,
          "local": 23
        },
        {
          "id": "P6-PLS-0099",
          "name": "El Barrio",
          "source": "spotify",
          "theme": "Latin hip-hop et Cuba urbain",
          "genres": "latin, rap, hip-hop, hip hop, latin pop",
          "artists": "Ogguere, Orishas, KAROL G, Danay Suárez",
          "energy": 0.62,
          "valence": 0.89,
          "spotify": 5,
          "local": 4
        },
        {
          "id": "P6-PLS-0085",
          "name": "Electronica",
          "source": "spotify",
          "theme": "Électronica downtempo sombre et texturée (V0.21)",
          "genres": "electronic, downtempo, chillout, trip-hop, electronica",
          "artists": "Guts, Modeselektor, Romare, Quantic",
          "energy": 0.55,
          "valence": 0.21,
          "spotify": 15,
          "local": 8
        },
        {
          "id": "P6-PLS-0090",
          "name": "Esperanza",
          "source": "spotify",
          "theme": "Nueva canción et grandes voix engagées d'Amérique latine",
          "genres": "folk, world, argentina, latin, nueva cancion",
          "artists": "Mercedes Sosa, Maria Teresa De Noronha, Hnas. Mendoza Suasti, Vivir Quintana",
          "energy": null,
          "valence": null,
          "spotify": 10,
          "local": 8
        },
        {
          "id": "P6-PLS-0096",
          "name": "Favela Frenzy",
          "source": "spotify",
          "theme": "Baile funk carioca festif",
          "genres": "brazilian, brazil, funk, pop, brasil",
          "artists": "MC Kevinho, Randy Nota Loca, Ape Drums, The Gardener",
          "energy": 0.78,
          "valence": 0.67,
          "spotify": 4,
          "local": 3
        },
        {
          "id": "P6-PLS-0082",
          "name": "Filmmaking ",
          "source": "spotify",
          "theme": "Micro-sélection d'ambiances pour l'image/tournage",
          "genres": "dub, trip-hop, cumbia, ambient, dutch",
          "artists": "BICEP, El Búho, Devendra Banhart, Tadao Hayashi Harp Trio",
          "energy": null,
          "valence": null,
          "spotify": 1,
          "local": 1
        },
        {
          "id": "P6-PLS-0053",
          "name": "Firefly",
          "source": "spotify",
          "theme": "Piano néoclassique contemplatif (Pamart, Tiersen, Frahm)",
          "genres": "contemporary classical, instrumental, classical, piano, pianist",
          "artists": "Sofiane Pamart, Yann Tiersen, KayThePianist, Nils Frahm",
          "energy": null,
          "valence": null,
          "spotify": 30,
          "local": 7
        },
        {
          "id": "P6-PLS-0083",
          "name": "Flower Power",
          "source": "spotify",
          "theme": "Rock psychédélique 60s, esprit Woodstock",
          "genres": "classic rock, rock, 60s, british invasion, mod",
          "artists": "The Animals, The Doors, Jefferson Airplane, Jimi Hendrix",
          "energy": null,
          "valence": null,
          "spotify": 50,
          "local": 34
        },
        {
          "id": "P6-PLS-0057",
          "name": "Fluo Kids",
          "source": "spotify",
          "theme": "French touch et électro fluo des années 2000 (Justice, DJ Mehdi)",
          "genres": "electronic, electronica, house, electro, techno",
          "artists": "Daft Punk, Justice, DJ Mehdi, Breakbot",
          "energy": 0.74,
          "valence": 0.81,
          "spotify": 52,
          "local": 26
        },
        {
          "id": "P6-PLS-0097",
          "name": "Fresh Rhymes",
          "source": "spotify",
          "theme": "Old-school hip-hop et électro-funk des origines",
          "genres": "funk, hip-hop, electro, electronic, hip hop",
          "artists": "DJ Jazzy Jeff & The Fresh Prince, Dream Warriors, Afrika Bambaataa, James Brown",
          "energy": null,
          "valence": null,
          "spotify": 9,
          "local": 8
        },
        {
          "id": "P6-PLS-0063",
          "name": "Grunge Glory",
          "source": "spotify",
          "theme": "Rock alternatif et grunge années 90",
          "genres": "rock, alternative rock, alternative, funk rock, funk",
          "artists": "The Offspring, Nirvana, Red Hot Chili Peppers, The White Stripes",
          "energy": null,
          "valence": null,
          "spotify": 29,
          "local": 27
        },
        {
          "id": "P6-PLS-0059",
          "name": "Guinguette",
          "source": "spotify",
          "theme": "Patrimoine chanson française : Brel, Brassens, Piaf",
          "genres": "chanson francaise, chanson, francais, singer-songwriter, jazz",
          "artists": "Jacques Brel, Georges Brassens, Édith Piaf, Boris Vian",
          "energy": null,
          "valence": null,
          "spotify": 48,
          "local": 41
        },
        {
          "id": "P6-PLS-0033",
          "name": "Gypsy Caravan",
          "source": "spotify",
          "theme": "Fanfares balkaniques et swing tzigane",
          "genres": "balkan, gypsy, world, folk, brass",
          "artists": "Fanfare Ciocarlia, Goran Bregović, Balkan Beat Box, Shantel",
          "energy": null,
          "valence": null,
          "spotify": 131,
          "local": 79
        },
        {
          "id": "P6-PLS-0092",
          "name": "Habibi",
          "source": "spotify",
          "theme": "Raï et pop arabe (Khaled, Taha, Cheb Mami)",
          "genres": "world, algerian, algeria, rai, arabic",
          "artists": "Rachid Taha, Faudel, Khaled, Cheb Mami",
          "energy": null,
          "valence": null,
          "spotify": 17,
          "local": 12
        },
        {
          "id": "P6-PLS-0068",
          "name": "Heat Flow",
          "source": "spotify",
          "theme": "Reggaeton et latin trap caliente",
          "genres": "latin, reggaeton, trap, puerto rico, latin pop",
          "artists": "Bad Bunny, Nicky Jam, J Balvin, KAROL G",
          "energy": null,
          "valence": null,
          "spotify": 56,
          "local": 29
        },
        {
          "id": "P6-PLS-0081",
          "name": "Hypnotic Chill",
          "source": "spotify",
          "theme": "Électronica hypnotique mi-teinte (Bonobo, Jamie xx)",
          "genres": "electronic, electronica, downtempo, chillout, trip-hop",
          "artists": "Bonobo, Four Tet, Jamie xx, Nicolas Jaar",
          "energy": 0.57,
          "valence": 0.35,
          "spotify": 12,
          "local": 9
        },
        {
          "id": "P6-PLS-0055",
          "name": "Idoles Des Jeunes",
          "source": "spotify",
          "theme": "Yéyé et chanson 60s (Gainsbourg, Hallyday, Dutronc)",
          "genres": "pop, french pop, chanson francaise, chanson, 60s",
          "artists": "Serge Gainsbourg, Brigitte Bardot, Johnny Hallyday, Jacques Dutronc",
          "energy": null,
          "valence": null,
          "spotify": 42,
          "local": 14
        },
        {
          "id": "P6-PLS-0088",
          "name": "Imperial Dusk",
          "source": "spotify",
          "theme": "Downtempo crépusculaire et lounge",
          "genres": "electronic, downtempo, chillout, trip-hop, lounge",
          "artists": "Guts, CloZee, Bonobo, Protassov",
          "energy": null,
          "valence": null,
          "spotify": 8,
          "local": 6
        },
        {
          "id": "P6-PLS-0093",
          "name": "Indietronica",
          "source": "spotify",
          "theme": "Électro-indie 2000s-2010s (Ratatat, The Knife)",
          "genres": "electronic, electronica, indie, experimental, electro",
          "artists": "Ratatat, The Knife, Santigold, Flight Facilities",
          "energy": null,
          "valence": null,
          "spotify": 17,
          "local": 10
        },
        {
          "id": "P6-PLS-0071",
          "name": "Jukebox",
          "source": "spotify",
          "theme": "Rock'n'roll, doo-wop et rhythm'n'blues 50s",
          "genres": "soul, rhythm and blues, blues, funk, jazz",
          "artists": "Elvis Presley, The Drifters, Jackie Wilson, Chuck Berry",
          "energy": null,
          "valence": null,
          "spotify": 62,
          "local": 46
        },
        {
          "id": "P6-PLS-0005",
          "name": "Klub der Visionare (R)",
          "source": "spotify",
          "theme": "House/techno minimale berlinoise, esprit du club éponyme",
          "genres": "electronic, house, techno, deep house, minimal",
          "artists": "Daft Punk, Nicolas Jaar, Red Axes, Peggy Gou",
          "energy": 0.69,
          "valence": 0.48,
          "spotify": 272,
          "local": 175
        },
        {
          "id": "P6-PLS-0002",
          "name": "La Zad",
          "source": "spotify",
          "theme": "Chanson rebelle, reggae alternatif et rock contestataire français",
          "genres": "chanson francaise, reggae, alternative, rock, latin",
          "artists": "Manu Chao, Tryo, Mano Negra, Noir Désir",
          "energy": 0.8,
          "valence": 0.51,
          "spotify": 55,
          "local": 47
        },
        {
          "id": "P6-PLS-0109",
          "name": "Liked Songs",
          "source": "spotify",
          "theme": "",
          "genres": "",
          "artists": "",
          "energy": null,
          "valence": null,
          "spotify": 1,
          "local": 0
        },
        {
          "id": "P6-PLS-0110",
          "name": "Lonesome Guitar",
          "source": "spotify",
          "theme": "Graine autour de The Cat Empire (3 titres)",
          "genres": "ska, jazz, funk, australian, alternative",
          "artists": "The Cat Empire",
          "energy": null,
          "valence": null,
          "spotify": 3,
          "local": 0
        },
        {
          "id": "P6-PLS-0017",
          "name": "Low Rider",
          "source": "spotify",
          "theme": "G-funk et rap west coast, chrome et soleil",
          "genres": "hip-hop, rap, hip hop, gangsta rap, west coast",
          "artists": "Dr. Dre, Snoop Dogg, The Pharcyde, 2Pac",
          "energy": null,
          "valence": null,
          "spotify": 32,
          "local": 22
        },
        {
          "id": "P6-PLS-0025",
          "name": "Lush Harmonies",
          "source": "spotify",
          "theme": "R&B/hip-hop mainstream 2000s, énergie maximale",
          "genres": "hip-hop, hip hop, rap, rnb, pop",
          "artists": "Alicia Keys, Snoop Dogg, Akon, Busta Rhymes",
          "energy": 0.95,
          "valence": 0.51,
          "spotify": 104,
          "local": 55
        },
        {
          "id": "P6-PLS-0075",
          "name": "Mes titres Shazam",
          "source": "spotify",
          "theme": "Capture Shazam automatique : house/pop hétéroclite — vivier à trier",
          "genres": "house, electronic, germany, pop, reggae",
          "artists": "Mr. Belt & Wezol, Peter Tosh, Rampa, chuala",
          "energy": null,
          "valence": null,
          "spotify": 57,
          "local": 3
        },
        {
          "id": "P6-PLS-0049",
          "name": "Motocicleta",
          "source": "spotify",
          "theme": "Folk indé contemplatif, énergie minimale (E0.18)",
          "genres": "indie, folk, singer-songwriter, freak folk, indie folk",
          "artists": "Devendra Banhart, Hermanos Gutiérrez, CocoRosie, Taalbi Brothers",
          "energy": 0.18,
          "valence": 0.39,
          "spotify": 28,
          "local": 21
        },
        {
          "id": "P6-PLS-0107",
          "name": "Musashi",
          "source": "spotify",
          "theme": "Musique traditionnelle japonaise au koto (2 titres)",
          "genres": "nan",
          "artists": "Koto Music of Japan, Kouta Katsutaro",
          "energy": null,
          "valence": null,
          "spotify": 2,
          "local": 1
        },
        {
          "id": "P6-PLS-0026",
          "name": "My Playlist #113",
          "source": "spotify",
          "theme": "Disco-house franco-brésilienne (mériterait un vrai nom)",
          "genres": "electronic, house, funk, disco, italy",
          "artists": "João Selva, Clea Vincent, Amphitryon, Daniel Vangarde",
          "energy": 0.68,
          "valence": 0.68,
          "spotify": 24,
          "local": 0
        },
        {
          "id": "P6-PLS-0022",
          "name": "My Playlist #155",
          "source": "spotify",
          "theme": "Grand réservoir groove : funk, soul, jazz, downtempo (à renommer)",
          "genres": "electronic, funk, soul, jazz, downtempo",
          "artists": "Guts, Hermanos Gutiérrez, Quantic, Pongo",
          "energy": 0.68,
          "valence": 0.7,
          "spotify": 357,
          "local": 58
        },
        {
          "id": "P6-PLS-0012",
          "name": "My Shazam Tracks",
          "source": "spotify",
          "theme": "Capture Shazam automatique : rap/pop récent — vivier à trier",
          "genres": "hip-hop, rap, electronic, hip hop, pop",
          "artists": "Young Miko, Lithe, BJF, DYSTINCT",
          "energy": 0.53,
          "valence": 0.09,
          "spotify": 46,
          "local": 3
        },
        {
          "id": "P6-PLS-0103",
          "name": "Napoli",
          "source": "spotify",
          "theme": "Clin d'œil italien : italo-disco et funk transalpin",
          "genres": "electronic, disco, italian, italo disco, funk",
          "artists": "Pino D'Angiò, Ivan Conti, Huaira, Minuk",
          "energy": 0.75,
          "valence": 0.75,
          "spotify": 3,
          "local": 2
        },
        {
          "id": "P6-PLS-0007",
          "name": "Nostalgie Urbaine",
          "source": "spotify",
          "theme": "Rap français années 90, l'âge d'or (IAM, Fabe, Oxmo)",
          "genres": "hip-hop, rap, french rap, rap francais, french hip-hop",
          "artists": "IAM, Cut Killer, Fabe, Oxmo Puccino",
          "energy": 0.71,
          "valence": 0.67,
          "spotify": 292,
          "local": 155
        },
        {
          "id": "P6-PLS-0032",
          "name": "Onda Cubana",
          "source": "spotify",
          "theme": "Son cubain et salsa classique",
          "genres": "salsa, latin, cuba, world, son cubano",
          "artists": "Celia Cruz, Papaito, Peruchin, Orquesta Riverside",
          "energy": null,
          "valence": null,
          "spotify": 40,
          "local": 28
        },
        {
          "id": "P6-PLS-0019",
          "name": "Orbiting Souls",
          "source": "spotify",
          "theme": "Trip-hop spatial et downtempo planant",
          "genres": "electronic, downtempo, chillout, trip-hop, hip-hop",
          "artists": "Moby, Bonobo, Morcheeba, Andreya Triana",
          "energy": 0.52,
          "valence": 0.55,
          "spotify": 84,
          "local": 51
        },
        {
          "id": "P6-PLS-0036",
          "name": "Organic House",
          "source": "spotify",
          "theme": "House organique et désertique, transe lente",
          "genres": "electronic, house, latin, peru, world",
          "artists": "Novalima, Acid Pauli, Be Svendsen, Tebra",
          "energy": 0.69,
          "valence": 0.34,
          "spotify": 24,
          "local": 15
        },
        {
          "id": "P6-PLS-0029",
          "name": "Orishas",
          "source": "spotify",
          "theme": "MPB et bossa nova : le Brésil classique",
          "genres": "brazilian, brazil, brasil, mpb, bossa nova",
          "artists": "Jorge Ben Jor, Antônio Carlos Jobim, Elis Regina, Caetano Veloso",
          "energy": 0.59,
          "valence": 0.72,
          "spotify": 255,
          "local": 168
        },
        {
          "id": "P6-PLS-0086",
          "name": "Paperplane",
          "source": "spotify",
          "theme": "Électro-pop indé ludique (Hot Chip, Metronomy)",
          "genres": "electronic, electronica, electro, indie, experimental",
          "artists": "The Knife, Hot Chip, Justice, Metronomy",
          "energy": 0.64,
          "valence": 0.65,
          "spotify": 27,
          "local": 16
        },
        {
          "id": "P6-PLS-0066",
          "name": "Pirate Radio",
          "source": "spotify",
          "theme": "Rock 60s british invasion et proto-punk",
          "genres": "rock, classic rock, 60s, british invasion, mod",
          "artists": "The Kinks, The Animals, The Clash, David Bowie",
          "energy": null,
          "valence": null,
          "spotify": 128,
          "local": 73
        },
        {
          "id": "P6-PLS-0089",
          "name": "Pula's Harbor",
          "source": "spotify",
          "theme": "Dub-soul décontracté, esprit Fat Freddy's Drop",
          "genres": "reggae, dub, electronic, chillout, soul",
          "artists": "Fat Freddy's Drop, Flox, Blundetto, Easy Star All-Stars",
          "energy": null,
          "valence": null,
          "spotify": 9,
          "local": 7
        },
        {
          "id": "P6-PLS-0102",
          "name": "Radio Future",
          "source": "spotify",
          "theme": "Big beat/électro nerveuse, esprit jeu vidéo (E0.98 V0.11)",
          "genres": "electronic, house, electronica, big beat, dance",
          "artists": "Fatboy Slim, The Prodigy, Hideki Naganuma, The Chemical Brothers",
          "energy": 0.98,
          "valence": 0.11,
          "spotify": 7,
          "local": 4
        },
        {
          "id": "P6-PLS-0035",
          "name": "Rainforest Rhaspody",
          "source": "spotify",
          "theme": "Cumbia digitale et folktronica andine (El Búho, Nicola Cruz)",
          "genres": "electronic, latin, argentina, cumbia, folk",
          "artists": "El Búho, Nicola Cruz, Yeahman, Barrio Lindo",
          "energy": 0.53,
          "valence": 0.49,
          "spotify": 201,
          "local": 114
        },
        {
          "id": "P6-PLS-0023",
          "name": "Rave Nation",
          "source": "spotify",
          "theme": "Doublon/extension eurodance 90s de l'autre Rave Nation — à fusionner ?",
          "genres": "electronic, dance, house, disco, pop",
          "artists": "Dr. Alban, Haddaway, Peggy Gou, Robin S",
          "energy": 0.9,
          "valence": 0.95,
          "spotify": 52,
          "local": 23
        },
        {
          "id": "P6-PLS-0067",
          "name": "Reading",
          "source": "spotify",
          "theme": "Lofi beats pour lire et travailler",
          "genres": "nan",
          "artists": "Laffey, Hoogway, Pierson Booth, Late Night Tones",
          "energy": null,
          "valence": null,
          "spotify": 211,
          "local": 0
        },
        {
          "id": "P6-PLS-0098",
          "name": "Red Beach",
          "source": "spotify",
          "theme": "Micro-sélection beats instrumentaux chill",
          "genres": "electronic, downtempo, hip-hop, trip-hop, instrumental hip-hop",
          "artists": "Desmond Cheese, DJ Vadim, Guts, Masego",
          "energy": null,
          "valence": null,
          "spotify": 3,
          "local": 1
        },
        {
          "id": "P6-PLS-0108",
          "name": "Reflets de France",
          "source": "spotify",
          "theme": "Folk régional français : Bretagne et Pays basque",
          "genres": "nan",
          "artists": "Tri Yann, Ontuak, Marc Lartigau, Gorka Robles",
          "energy": null,
          "valence": null,
          "spotify": 10,
          "local": 9
        },
        {
          "id": "P6-PLS-0052",
          "name": "Roaring Twenties",
          "source": "spotify",
          "theme": "Jazz des années folles, swing et cabaret",
          "genres": "lounge, nu-jazz, downtempo, jazz, nu jazz",
          "artists": "The Washboard Rhythm Kings, Benny Goodman, Cab Calloway, Louis Armstrong",
          "energy": null,
          "valence": null,
          "spotify": 28,
          "local": 8
        },
        {
          "id": "P6-PLS-0080",
          "name": "Rock",
          "source": "spotify",
          "theme": "Classic rock et blues rock intemporels",
          "genres": "rock, classic rock, blues, blues rock, 60s",
          "artists": "Johnny Cash, Fleetwood Mac, The Kinks, Paul Simon",
          "energy": null,
          "valence": null,
          "spotify": 34,
          "local": 21
        },
        {
          "id": "P6-PLS-0054",
          "name": "Rockers Serenade",
          "source": "spotify",
          "theme": "Roots reggae et rocksteady jamaïcain profond",
          "genres": "reggae, jamaica, roots reggae, roots, dub",
          "artists": "Alton Ellis, Ken Boothe, Gregory Isaacs, Horace Andy",
          "energy": null,
          "valence": null,
          "spotify": 314,
          "local": 128
        },
        {
          "id": "P6-PLS-0039",
          "name": "Rush Hour (R)",
          "source": "spotify",
          "theme": "Afro-funk et tropical groove haute énergie, taillé pour le dancefloor",
          "genres": "electronic, funk, world, african, afrobeat",
          "artists": "Guts, Voilaaa, The Mauskovic Dance Band, Antibalas",
          "energy": 0.73,
          "valence": 0.77,
          "spotify": 129,
          "local": 81
        },
        {
          "id": "P6-PLS-0042",
          "name": "Shamanic",
          "source": "spotify",
          "theme": "Folk andin et downtempo chamanique, organique et rituel",
          "genres": "electronic, latin, folk, acoustic, latinoamerica",
          "artists": "Huaira, Danit, Lulacruza, Rodrigo Gallardo",
          "energy": 0.44,
          "valence": 0.42,
          "spotify": 29,
          "local": 16
        },
        {
          "id": "P6-PLS-0095",
          "name": "Shamrock",
          "source": "spotify",
          "theme": "Folk irlandais et punk celtique",
          "genres": "nan",
          "artists": "The Pogues, The Irish Rovers, Dropkick Murphys, The Dubliners",
          "energy": null,
          "valence": null,
          "spotify": 8,
          "local": 8
        },
        {
          "id": "P6-PLS-0024",
          "name": "Shaolin Temple",
          "source": "spotify",
          "theme": "Rap east coast New York : Wu-Tang et affiliés",
          "genres": "hip-hop, rap, hip hop, east coast rap, new york",
          "artists": "Wu-Tang Clan, Method Man, Ol' Dirty Bastard, GZA",
          "energy": 0.64,
          "valence": 0.61,
          "spotify": 215,
          "local": 151
        },
        {
          "id": "P6-PLS-0046",
          "name": "Signature Gen 2",
          "source": "spotify",
          "theme": "Électro-world : Afrique et beats downtempo",
          "genres": "electronic, world, hip-hop, african, mali",
          "artists": "DJ Vadim, Al'Tarba, Deceptikon, Rokia Traoré",
          "energy": 0.63,
          "valence": 0.8,
          "spotify": 54,
          "local": 38
        },
        {
          "id": "P6-PLS-0008",
          "name": "Signature Gen 3",
          "source": "spotify",
          "theme": "Mix générationnel actuel : rap, latin, électro",
          "genres": "electronic, rap, pop, hip-hop, latin",
          "artists": "Maes, Bad Bunny, Young Miko, Chancha Via Circuito",
          "energy": 0.59,
          "valence": 0.68,
          "spotify": 114,
          "local": 78
        },
        {
          "id": "P6-PLS-0079",
          "name": "Skinhead Symphony",
          "source": "spotify",
          "theme": "Ska et rocksteady originels 60s",
          "genres": "ska, reggae, rocksteady, skinhead reggae, jamaica",
          "artists": "Prince Buster, Bad Manners, Laurel Aitken, Sir Lord Comic",
          "energy": null,
          "valence": null,
          "spotify": 16,
          "local": 8
        },
        {
          "id": "P6-PLS-0087",
          "name": "Skuu",
          "source": "spotify",
          "theme": "Rap/R&B US des années 2000",
          "genres": "hip-hop, rap, hip hop, rnb, trap",
          "artists": "Fat Joe, Drake, Ludacris, Ashanti",
          "energy": null,
          "valence": null,
          "spotify": 20,
          "local": 3
        },
        {
          "id": "P6-PLS-0064",
          "name": "Smoothy",
          "source": "spotify",
          "theme": "Neo-soul et funk moderne velouté",
          "genres": "soul, funk, electronic, hip-hop, hip hop",
          "artists": "Ibeyi, Cookin' On 3 Burners, Nneka, Alice Russell",
          "energy": null,
          "valence": null,
          "spotify": 47,
          "local": 21
        },
        {
          "id": "P6-PLS-0101",
          "name": "Space Cowboy",
          "source": "spotify",
          "theme": "Croisement hip-hop/indé spatial autour de Gorillaz",
          "genres": "hip-hop, indie, electronic, alternative, rock",
          "artists": "Gorillaz, De La Soul, Space Monkeyz, Neneh Cherry",
          "energy": null,
          "valence": null,
          "spotify": 16,
          "local": 7
        },
        {
          "id": "P6-PLS-0015",
          "name": "Space Lullaby",
          "source": "spotify",
          "theme": "Instrumental hip-hop et trip-hop berceur",
          "genres": "electronic, trip-hop, downtempo, hip-hop, instrumental hip-hop",
          "artists": "Bonobo, Blockhead, DJ Shadow, Amon Tobin",
          "energy": null,
          "valence": null,
          "spotify": 108,
          "local": 92
        },
        {
          "id": "P6-PLS-0056",
          "name": "Starmania 2.0",
          "source": "spotify",
          "theme": "Variété française 80s dramatique (Balavoine, Farmer)",
          "genres": "pop, chanson francaise, 80s, french pop, daniel balavoine",
          "artists": "Daniel Balavoine, Christophe, Mylène Farmer, France Gall",
          "energy": null,
          "valence": null,
          "spotify": 40,
          "local": 31
        },
        {
          "id": "P6-PLS-0048",
          "name": "Strutopia",
          "source": "spotify",
          "theme": "Acid jazz et rare groove qui parade",
          "genres": "funk, soul, jazz, acid jazz, electronic",
          "artists": "Bacao Rhythm & Steel Band, Charles Kynard, The Quantic Soul Orchestra, Quantic",
          "energy": null,
          "valence": null,
          "spotify": 24,
          "local": 9
        },
        {
          "id": "P6-PLS-0060",
          "name": "Suprême 3rd Gen",
          "source": "spotify",
          "theme": "Rap actuel entre France et Caraïbes",
          "genres": "rap, hip-hop, hip hop, electronic, pop",
          "artists": "Jul, Poupie, Wyclef Jean, John Forte",
          "energy": 0.65,
          "valence": 0.6,
          "spotify": 24,
          "local": 12
        },
        {
          "id": "P6-PLS-0014",
          "name": "Suprême Gen 2",
          "source": "spotify",
          "theme": "World électronique : blues du désert et Afrique",
          "genres": "electronic, african, world, desert blues, downtempo",
          "artists": "Fakear, Idir, Bombino, Bonga",
          "energy": 0.6,
          "valence": 0.69,
          "spotify": 40,
          "local": 24
        },
        {
          "id": "P6-PLS-0038",
          "name": "Synthetic Sea",
          "source": "spotify",
          "theme": "New wave et synth-pop 80s",
          "genres": "new wave, 80s, post-punk, alternative, rock",
          "artists": "Depeche Mode, New Order, Pet Shop Boys, The Cure",
          "energy": null,
          "valence": null,
          "spotify": 20,
          "local": 15
        },
        {
          "id": "P6-PLS-0104",
          "name": "Trip Hop",
          "source": "spotify",
          "theme": "Le canon trip-hop : scratches et basses rondes",
          "genres": "electronic, downtempo, trip-hop, hip-hop, chillout",
          "artists": "Mo' Horizons, DJ Vadim, Roots Manuva, Kid Koala",
          "energy": null,
          "valence": null,
          "spotify": 5,
          "local": 3
        },
        {
          "id": "P6-PLS-0013",
          "name": "Trippy Abastraction",
          "source": "spotify",
          "theme": "Trip-hop français psychédélique (Chinese Man, Fakear)",
          "genres": "electronic, trip-hop, downtempo, chillout, hip-hop",
          "artists": "Bonobo, Chinese Man, Fakear, Guts",
          "energy": null,
          "valence": null,
          "spotify": 102,
          "local": 88
        },
        {
          "id": "P6-PLS-0020",
          "name": "UK Culture",
          "source": "spotify",
          "theme": "Bass music UK : garage, dubstep, drum'n'bass",
          "genres": "electronic, dubstep, house, future garage, uk garage",
          "artists": "Ed Solo, Maribou State, Joker, Deekline",
          "energy": 0.73,
          "valence": 0.39,
          "spotify": 157,
          "local": 103
        },
        {
          "id": "P6-PLS-0061",
          "name": "Wandering Dunes",
          "source": "spotify",
          "theme": "Blues du désert touareg (Tinariwen, Bombino)",
          "genres": "african, world, mali, blues, desert blues",
          "artists": "Tinariwen, Ali Farka Touré, Terakaft, Bombino",
          "energy": 0.71,
          "valence": 0.8,
          "spotify": 30,
          "local": 23
        },
        {
          "id": "P6-PLS-0074",
          "name": "Warehouse ",
          "source": "spotify",
          "theme": "Techno/minimale de warehouse, hypnotique et sombre",
          "genres": "electronic, techno, minimal, house, minimal techno",
          "artists": "Solomun, DJ Koze, Blawan, Recondite",
          "energy": 0.63,
          "valence": 0.34,
          "spotify": 22,
          "local": 16
        }
      ],
      "files_matched": 84133
    },
    "scan": {
      "files": 85040,
      "size": 441799444066,
      "size_h": "411.5 Go",
      "no_tags": 23167,
      "no_artist": 44439,
      "no_album": 32882,
      "folders": [
        [
          "music/workspace",
          67763
        ],
        [
          "music/library",
          17252
        ],
        [
          "music/playlists",
          24
        ],
        [
          "music/tracks",
          1
        ]
      ],
      "extensions": [
        [
          ".mp3",
          84890
        ],
        [
          ".wav",
          58
        ],
        [
          ".flac",
          32
        ],
        [
          ".wma",
          29
        ],
        [
          ".m4a",
          21
        ],
        [
          ".ogg",
          10
        ]
      ]
    },
    "scan_pre": {
      "files": 88223,
      "size": 513107202605,
      "size_h": "477.9 Go",
      "no_tags": 24015,
      "no_artist": 44992,
      "no_album": 34507,
      "folders": [
        [
          "music/workspace",
          68933
        ],
        [
          "music/library",
          18544
        ],
        [
          "radio/Radio-Library",
          575
        ],
        [
          "downloads/Book Club Radio",
          125
        ],
        [
          "music/playlists",
          24
        ],
        [
          "downloads/Big Business HQ",
          21
        ],
        [
          "music/tracks",
          1
        ]
      ],
      "extensions": [
        [
          ".mp3",
          88073
        ],
        [
          ".wav",
          58
        ],
        [
          ".flac",
          32
        ],
        [
          ".wma",
          29
        ],
        [
          ".m4a",
          21
        ],
        [
          ".ogg",
          10
        ]
      ]
    },
    "fingerprint": {
      "done": 3420,
      "total": 44529
    },
    "exports": [],
    "spotify_dupes": {
      "intra": 677,
      "inter": 2972
    },
    "local_quarantined": 2464,
    "local_dupes_pending": 0,
    "proposals": {
      "monoliths": 26,
      "splits": 215,
      "moves": 366
    }
  }
};
