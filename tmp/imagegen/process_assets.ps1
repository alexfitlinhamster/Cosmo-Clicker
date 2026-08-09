Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public static class SpriteProcessor {
    static int Dist(Color a, Color b) {
        int dr=a.R-b.R, dg=a.G-b.G, db=a.B-b.B;
        return (int)Math.Sqrt(dr*dr + dg*dg + db*db);
    }

    public static void Process(string input, string output) {
        using (var source = new Bitmap(input)) {
            var work = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb);
            using (var g = Graphics.FromImage(work)) g.DrawImageUnscaled(source, 0, 0);
            var key = work.GetPixel(0, 0);
            bool greenKey = key.G > 180 && key.G > key.R + 70 && key.G > key.B + 70;
            var visited = new bool[work.Width * work.Height];
            var queue = new Queue<Point>();
            for (int x=0; x<work.Width; x++) { queue.Enqueue(new Point(x,0)); queue.Enqueue(new Point(x,work.Height-1)); }
            for (int y=0; y<work.Height; y++) { queue.Enqueue(new Point(0,y)); queue.Enqueue(new Point(work.Width-1,y)); }
            while (queue.Count > 0) {
                var p=queue.Dequeue(); int i=p.Y*work.Width+p.X;
                if (visited[i]) continue;
                var c=work.GetPixel(p.X,p.Y);
                bool background = greenKey
                    ? c.G > 105 && c.G > c.R + 28 && c.G > c.B + 28
                    : Dist(c,key) < 75;
                if (!background) continue;
                visited[i]=true;
                work.SetPixel(p.X,p.Y,Color.Transparent);
                if(p.X>0) queue.Enqueue(new Point(p.X-1,p.Y));
                if(p.X+1<work.Width) queue.Enqueue(new Point(p.X+1,p.Y));
                if(p.Y>0) queue.Enqueue(new Point(p.X,p.Y-1));
                if(p.Y+1<work.Height) queue.Enqueue(new Point(p.X,p.Y+1));
            }
            int minX=work.Width,minY=work.Height,maxX=-1,maxY=-1;
            for(int y=0;y<work.Height;y++) for(int x=0;x<work.Width;x++) if(work.GetPixel(x,y).A>8) {
                minX=Math.Min(minX,x); minY=Math.Min(minY,y); maxX=Math.Max(maxX,x); maxY=Math.Max(maxY,y);
            }
            if(maxX<minX) throw new Exception("No subject found: "+input);
            var crop=new Rectangle(minX,minY,maxX-minX+1,maxY-minY+1);
            float scale=Math.Min(232f/crop.Width,232f/crop.Height);
            int dw=(int)Math.Round(crop.Width*scale), dh=(int)Math.Round(crop.Height*scale);
            using(var result=new Bitmap(256,256,PixelFormat.Format32bppArgb))
            using(var g=Graphics.FromImage(result)) {
                g.Clear(Color.Transparent); g.CompositingMode=CompositingMode.SourceCopy;
                g.CompositingQuality=CompositingQuality.HighQuality; g.InterpolationMode=InterpolationMode.HighQualityBicubic;
                g.SmoothingMode=SmoothingMode.HighQuality; g.PixelOffsetMode=PixelOffsetMode.HighQuality;
                g.DrawImage(work,new Rectangle((256-dw)/2,(256-dh)/2,dw,dh),crop,GraphicsUnit.Pixel);
                result.Save(output,ImageFormat.Png);
            }
            work.Dispose();
        }
    }
}
'@

$source = 'C:\Users\User\.codex\generated_images\019fe6ad-4d45-73b1-864a-5a9c46c86e59'
$dest = 'C:\Users\User\AndroidStudioProjects\MyApplication4\app\src\main\res\drawable'
$droneFiles = @(
 'exec-fbaaa376-f1b7-4096-acc6-8fc65c02f58a.png','exec-1486b489-41a7-43dc-be95-acccbdb6dd9d.png','exec-fab60f2d-a9e6-47df-82b8-83334715036c.png','exec-9cffffd4-0f67-4409-a950-956ef2e64e6e.png',
 'exec-45692465-3a0f-40a1-924e-9d0e7574c998.png','exec-4c7835ad-149d-4c43-88cc-aeef428ef67d.png','exec-3cf941c3-ed64-41de-bc41-b975ae325167.png','exec-2a5b86a4-68fe-40f3-a93b-1dc68782de3c.png',
 'exec-3594b37f-46ee-4411-8190-fe75407e67dd.png','exec-b3e46e13-c2cc-4430-b7f8-877ab66fe772.png','exec-0e127c98-225a-4fc4-a51e-0c74fab0420d.png','exec-74337c89-0f47-49da-ad1a-297c3903c198.png',
 'exec-cf64557e-1033-47e3-ab07-8e5d4c467a95.png','exec-6bf07fbe-8fd3-4591-acae-d5a954cdf562.png','exec-bc037e52-2ae4-4e29-992c-f354099dfbfc.png','exec-8804f19e-fc60-4320-952e-deec5171aa3d.png',
 'exec-40714326-5300-4d45-b26b-930077da14ab.png','exec-6a6f67d5-41f9-4626-9497-e1eff6c97880.png','exec-97081908-7a4b-4abd-ad72-78fb084e4f4e.png','exec-7cf7f224-088c-45be-9f3f-d58969b22720.png',
 'exec-f70eaafa-e5f3-4ec9-a6ea-8ccc20234af6.png','exec-1a16fee8-5db3-43af-b2c8-56474a43baee.png','exec-e129eb91-ed1f-4d38-bb27-6b40cad04c07.png','exec-148708b3-2196-44ff-a11e-c44b44681538.png','exec-5a3219d6-1833-47f9-a61a-472de7c1e74b.png'
)
$planetFiles = @(
 'exec-814a5c5c-552b-489a-8701-70862bab3f53.png','exec-c559139b-3301-404a-b555-3ef7623f9564.png','exec-c0e31c71-1a1e-4a62-9d5b-883f3cf97a21.png','exec-c95396a2-f9e9-4f13-bcfc-c20a069e7f42.png',
 'exec-b0991edf-e996-45a6-8a10-24d25054f397.png','exec-6e1ded53-4574-480a-aefe-ba9517350da2.png','exec-644d31ef-9200-4e01-b204-b4d45e085325.png','exec-5038ac92-ca74-47c6-a322-df181dc7f894.png',
 'exec-386c11c6-50f4-4023-b4a4-8dd04295d799.png','exec-5009abcc-780f-42de-939b-851ce87773b2.png','exec-ed36ef10-28c8-4be2-88c2-41bb0c262b6e.png','exec-44001d8e-01e9-4842-8e92-ca11912f8be1.png',
 'exec-a8aabd68-11b5-4973-84d3-00d164811df8.png','exec-26b26995-6efa-47c5-9dac-c8cddcbdce0c.png','exec-47de5281-b387-43fa-b2c7-20ab02e73a39.png','exec-bc70a772-dc01-41aa-94e4-6f5360a4a2f4.png'
)
for($i=0;$i -lt $droneFiles.Count;$i++) { [SpriteProcessor]::Process((Join-Path $source $droneFiles[$i]),(Join-Path $dest ('drone_{0:D2}_v2.png' -f ($i+5)))) }
for($i=0;$i -lt $planetFiles.Count;$i++) { [SpriteProcessor]::Process((Join-Path $source $planetFiles[$i]),(Join-Path $dest ('planet_{0}_v2.png' -f ($i+5)))) }
