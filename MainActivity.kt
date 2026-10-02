package com.resinacostos.app

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

data class Material(var name:String, var price:Double, var qty:Double, var unit:String)
data class SavedProduct(var name:String, var cost:Double, var sale:Double, var date:String)

class MainActivity : Activity() {
    val materials=mutableListOf<Material>()
    val products=mutableListOf<SavedProduct>()
    lateinit var content:LinearLayout
    lateinit var status:TextView

    override fun onCreate(b:Bundle?){super.onCreate(b); seed(); home()}
    fun seed(){
        materials.addAll(listOf(
            Material("Resina",250.0,1000.0,"ml"),
            Material("Pintura",45.0,100.0,"ml"),
            Material("Diamantina",30.0,50.0,"g"),
            Material("Palillos",25.0,100.0,"pza"),
            Material("Vinil",80.0,100.0,"cm"),
            Material("Molde",150.0,1.0,"pza")
        ))
    }
    fun base():LinearLayout{
        val l=LinearLayout(this); l.orientation=LinearLayout.VERTICAL; l.setPadding(24,24,24,24); l.setBackgroundColor(Color.rgb(248,246,251)); return l
    }
    fun btn(t:String, f:()->Unit)=Button(this).apply{text=t;setOnClickListener{f()}}
    fun txt(t:String,s:Float=16f)=TextView(this).apply{text=t;textSize=s;setPadding(0,10,0,10);setTextColor(Color.rgb(55,35,75))}
    fun edit(h:String,v:String="")=EditText(this).apply{hint=h;setText(v);setPadding(12,8,12,8)}
    fun show(root:LinearLayout){setContentView(ScrollView(this).apply{addView(root)})}

    fun home(){
        val r=base()
        r.addView(txt("🧮 COSTOS DE RESINA",27f))
        r.addView(txt("Calcula costos, precios y ganancias de tus productos.",16f))
        r.addView(btn("➕ Nueva cotización"){quote()})
        r.addView(btn("📦 Inventario de materiales"){inventory()})
        r.addView(btn("💾 Productos guardados"){saved()})
        r.addView(btn("🧾 Historial de cotizaciones"){history()})
        status=txt("");r.addView(status);show(r)
    }

    fun inventory(){
        val r=base();r.addView(txt("📦 INVENTARIO",25f))
        r.addView(txt("Registra precio de compra, cantidad y unidad."))
        materials.forEachIndexed{ i,m->
            val row=LinearLayout(this);row.orientation=LinearLayout.VERTICAL
            row.addView(txt("${m.name}: ${money(m.price)} por ${m.qty} ${m.unit}"))
            row.addView(btn("Editar / eliminar"){editMaterial(i)})
            r.addView(row)
        }
        r.addView(btn("➕ Agregar material"){editMaterial(-1)})
        r.addView(btn("← Inicio"){home()});show(r)
    }

    fun editMaterial(i:Int){
        val d=LinearLayout(this);d.orientation=LinearLayout.VERTICAL;d.setPadding(30,10,30,10)
        val n=edit("Nombre",if(i>=0)materials[i].name else "")
        val p=edit("Precio de compra",if(i>=0)materials[i].price.toString() else "")
        val q=edit("Cantidad comprada",if(i>=0)materials[i].qty.toString() else "")
        val u=edit("Unidad (ml, g, pza, cm)",if(i>=0)materials[i].unit else "")
        listOf(n,p,q,u).forEach{d.addView(it)}
        AlertDialog.Builder(this).setTitle(if(i>=0)"Editar material" else "Nuevo material").setView(d)
            .setPositiveButton("Guardar"){_,_->
                val m=Material(n.text.toString(),num(p),num(q),u.text.toString().ifBlank{"pza"})
                if(i>=0)materials[i]=m else materials.add(m)
            }.setNegativeButton("Cancelar",null)
            .setNeutralButton(if(i>=0)"Eliminar" else "Cerrar"){_,_->if(i>=0)materials.removeAt(i)}.show()
    }

    fun quote(){
        val r=base();r.addView(txt("🧾 NUEVA COTIZACIÓN",25f))
        val product=edit("Nombre del producto")
        val qty=edit("Cantidad de piezas","1")
        r.addView(product);r.addView(qty)
        r.addView(txt("MATERIALES UTILIZADOS"))
        val rows=mutableListOf<Pair<Spinner,EditText>>()
        materials.forEach{m->
            val row=LinearLayout(this);row.orientation=LinearLayout.HORIZONTAL
            val sp=Spinner(this);sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,materials.map{it.name})
            sp.setSelection(materials.indexOf(m))
            val used=edit("Cantidad usada","0")
            row.addView(sp,LinearLayout.LayoutParams(0,-2,1f));row.addView(used,LinearLayout.LayoutParams(0,-2,1f))
            r.addView(row);rows.add(sp to used)
        }
        val mins=edit("Minutos de trabajo","0"); val hour=edit("Valor de tu hora","80")
        val indirect=edit("Gastos indirectos por pieza","0");val margin=edit("Margen de ganancia %","50")
        r.addView(mins);r.addView(hour);r.addView(indirect);r.addView(margin)
        val out=txt("",19f);r.addView(btn("CALCULAR"){ 
            var mat=0.0
            rows.forEach{ val idx=it.first.selectedItemPosition; val m=materials[idx]; mat += if(m.qty>0) m.price/m.qty*num(it.second) else 0.0 }
            val labor=num(mins)/60*num(hour);val ind=num(indirect)*num(qty);val total=mat+labor+ind
            val sale=total*(1+num(margin)/100);val profit=sale-total
            out.text="""RESULTADO
Materiales: ${money(mat)}
Mano de obra: ${money(labor)}
Gastos indirectos: ${money(ind)}
COSTO TOTAL: ${money(total)}

Precio sugerido total: ${money(sale)}
Precio sugerido por pieza: ${money(if(num(qty)>0)sale/num(qty) else 0.0)}
Ganancia: ${money(profit)}"""
            products.add(SavedProduct(product.text.toString().ifBlank{"Producto sin nombre"},total,sale,now()))
        });r.addView(out)
        r.addView(btn("← Inicio"){home()});show(r)
    }

    fun saved(){
        val r=base();r.addView(txt("💾 PRODUCTOS GUARDADOS",25f))
        if(products.isEmpty())r.addView(txt("Todavía no hay productos guardados."))
        products.forEach{r.addView(txt("${it.name}\nCosto: ${money(it.cost)} | Venta: ${money(it.sale)}\n${it.date}"))}
        r.addView(btn("← Inicio"){home()});show(r)
    }
    fun history(){saved()}
    fun num(e:EditText)=e.text.toString().replace(",","." ).toDoubleOrNull()?:0.0
    fun money(x:Double)="$"+String.format(Locale.US,"%.2f",x)
    fun now()=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date())
}