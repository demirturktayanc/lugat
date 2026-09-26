"""Kelime destelerini doğrular ve app/src/main/assets/content.json dosyasını üretir."""
import json, os, re, sys, unicodedata
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from d_school import ILKOKUL, ORTAOKUL, LISE
from d_exam import YKS, YDS, HAZIRLIK
from d_dept import TIP, MUHENDISLIK, BILGISAYAR, HUKUK, ISLETME, PSIKOLOJI
from d_extra import PHRASAL, SEYAHAT
from facts import FACTS

POS = {"n", "v", "adj", "adv", "phr", "prep", "conj", "pron", "exp"}

# (id, başlık, alt başlık, grup, rozet, renk, kaynak)
DECKS = [
    ("ilkokul", "İlkokul", "Renkler, hayvanlar, aile ve ilk fiiller", "Okul", "A1", 0, ILKOKUL),
    ("ortaokul", "Ortaokul · LGS", "Günlük hayat, duygular ve çevre", "Okul", "A2", 1, ORTAOKUL),
    ("lise", "Lise", "Kariyer, toplum ve bağlaçlar", "Okul", "B1", 2, LISE),
    ("yks", "YKS · YDT", "Sınavda en sık çıkan akademik kelimeler", "Sınavlar", "B2", 3, YKS),
    ("yds", "YDS · YÖKDİL", "İleri düzey akademik kelime hazinesi", "Sınavlar", "C1", 4, YDS),
    ("hazirlik", "Üniversite Hazırlık", "Ders, ödev, sunum ve akademik yazma", "Üniversite", "B1+", 5, HAZIRLIK),
    ("tip", "Tıp ve Sağlık", "Tanı, tedavi ve hastane dili", "Bölümler", "TIP", 6, TIP),
    ("muhendislik", "Mühendislik", "Yapı, enerji, devre ve tasarım", "Bölümler", "MÜH", 7, MUHENDISLIK),
    ("bilgisayar", "Bilgisayar ve Yazılım", "Kod, veri ve güvenlik", "Bölümler", "BİL", 2, BILGISAYAR),
    ("hukuk", "Hukuk", "Mahkeme, sözleşme ve mevzuat", "Bölümler", "HUK", 4, HUKUK),
    ("isletme", "İşletme ve Ekonomi", "Finans, pazar ve girişimcilik", "Bölümler", "İŞL", 3, ISLETME),
    ("psikoloji", "Psikoloji ve Eğitim", "Zihin, davranış ve öğrenme", "Bölümler", "PSİ", 5, PSIKOLOJI),
    ("phrasal", "Phrasal Verbs", "Günlük İngilizcenin gizli anahtarı", "Ekstra", "PV", 0, PHRASAL),
    ("seyahat", "Seyahat İngilizcesi", "Havalimanı, otel, restoran ve yol tarifi", "Ekstra", "✈", 1, SEYAHAT),
]


def slug(s):
    s = unicodedata.normalize("NFKD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def contains_word(sentence, word):
    return re.search(r"(?<![A-Za-z])" + re.escape(word) + r"(?![A-Za-z])", sentence, re.IGNORECASE) is not None


errors, out_decks, total, blankable = [], [], 0, 0
for did, title, sub, group, badge, color, src in DECKS:
    words, seen = [], set()
    for ln, line in enumerate(l for l in src.strip().splitlines() if l.strip()):
        parts = [p.strip() for p in line.split("|")]
        if len(parts) != 5 or not all(parts):
            errors.append(f"{did}:{ln + 1} alan sayısı hatalı: {line}")
            continue
        en, pos, tr, ex_en, ex_tr = parts
        if pos not in POS:
            errors.append(f"{did}:{en} bilinmeyen tür {pos}")
        key = en.lower()
        if key in seen:
            errors.append(f"{did}: tekrar eden kelime {en}")
        seen.add(key)
        if not ex_en.endswith((".", "?", "!")) or not ex_tr.endswith((".", "?", "!")):
            errors.append(f"{did}:{en} örnek cümle noktalama")
        b = contains_word(ex_en, en)
        blankable += b
        words.append({"id": f"{did}:{slug(en)}", "en": en, "tr": tr, "pos": pos, "exEn": ex_en, "exTr": ex_tr})
    total += len(words)
    out_decks.append({"id": did, "title": title, "subtitle": sub, "group": group, "badge": badge,
                      "color": color, "words": words})

facts, fseen = [], set()
for en, pos, tr, fact, ex_en, ex_tr in FACTS:
    if en.lower() in fseen:
        errors.append(f"fact tekrar: {en}")
    fseen.add(en.lower())
    if pos not in POS or len(fact) < 40:
        errors.append(f"fact hatalı: {en}")
    facts.append({"en": en, "pos": pos, "tr": tr, "fact": fact, "exEn": ex_en, "exTr": ex_tr})

if errors:
    print("\n".join(errors))
    sys.exit(1)

dst = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "app", "src", "main", "assets", "content.json")
os.makedirs(os.path.dirname(dst), exist_ok=True)
with open(dst, "w", encoding="utf-8") as f:
    json.dump({"version": 1, "decks": out_decks, "facts": facts}, f, ensure_ascii=False, separators=(",", ":"))
print(f"OK: {len(out_decks)} deste, {total} kelime ({blankable} boşluk doldurmaya uygun), {len(facts)} günün kelimesi")
for d in out_decks:
    print(f"  {d['id']:<12} {len(d['words']):>3}")
