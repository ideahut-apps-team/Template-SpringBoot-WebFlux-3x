package net.ideahut.springboot.template.controller.test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ideahut.springboot.annotation.Public;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.FrameworkHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.WebFluxHelper;
import net.ideahut.springboot.report.ReportHandler;
import net.ideahut.springboot.report.ReportInput;
import net.ideahut.springboot.report.ReportType;
import net.ideahut.springboot.stream.ByteArrayInOutStream;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.object.ReportData;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSwapFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/*
 * Contoh penggunaan ReportHandler
 */
@ComponentScan
@RestController
@RequestMapping("/test/report")
class ReportController implements InitializingBean {
	
	private final AppProperties appProperties;
	private final ReportHandler reportHandler;
	
	private byte[] template;
	private byte[] imageHeader;
	private byte[] imageDetail;
	
	
	@Autowired
	ReportController(
		AppProperties appProperties,
		ReportHandler reportHandler
	) {
		this.appProperties = appProperties;
		this.reportHandler = reportHandler;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		String path = appProperties.getReportPath();
		ErrorHelper.throwBlank(path, () -> "AppProperties.reportPath is required");
		path = FrameworkHelper.replacePath(path);
		path = StringHelper.removeEnd(path, "/");
		template = FrameworkHelper.toByteArray(FrameworkHelper.getResource(path + "/sample.jasper"));
		imageHeader = FrameworkHelper.toByteArray(FrameworkHelper.getResource(path + "/tree1.png"));
		imageDetail = FrameworkHelper.toByteArray(FrameworkHelper.getResource(path + "/tree2.png"));
	}
	
	// contoh respon resource, memuat seluruh data ke memori
	@Public
	@GetMapping("/resource")
	public ResponseEntity<Mono<Resource>> resource(
		@RequestParam("name") String name
	) {
		ReportType type = getType(name);
		Mono<ByteArrayInputStream> mono = WebFluxHelper.fromCallable(() -> {
			ByteArrayInOutStream inOutStream = new ByteArrayInOutStream();
			export(type, inOutStream);
            OutputStreamWriter writer = new OutputStreamWriter(inOutStream);
            writer.flush();
            return inOutStream.getInputStream();
		});
		return ResponseEntity.ok()
		.contentType(MediaType.valueOf(type.getContentType()))
        .body(mono.flatMap(stream -> {
        	Resource resource = new InputStreamResource(stream);
            return Mono.just(resource);
        }));
	}
	
	// contoh respons stream, tidak memuat seluruh data ke memory
	@Public
	@GetMapping("/stream")
	public ResponseEntity<Flux<DataBuffer>> stream(
		@RequestParam("name") String name
	) {
		ReportType type = getType(name);
		Flux<DataBuffer> body = Flux.defer(() -> {
			try {
				PipedOutputStream pos = new PipedOutputStream();
				PipedInputStream pis = new PipedInputStream(pos, 8192);
				
				// Jalankan export sebagai Mono agar error-nya bisa ditangkap & diteruskan
				Mono<Void> export = WebFluxHelper.fromRunnable(() -> export(type, pos));

				// Baca stream secara bertahap (4 KB per chunk) dan pis ditutup saat selesai / cancel
				Flux<DataBuffer> dataStream = DataBufferUtils
				.readInputStream(() -> pis, DefaultDataBufferFactory.sharedInstance, 4096)
				.subscribeOn(WebFluxHelper.getScheduler())
				.doFinally(signalType -> {
					try { pis.close(); } catch (Exception ignored) { /**/ }
				});

				// Gabungkan eksekusi export dan dataStream secara paralel
				// Jika export error, dataStream otomatis dibatalkan dan error dikirim ke subscriber
				return Flux.merge(export.cast(DataBuffer.class), dataStream);

			} catch (Exception e) {
				return Flux.error(ErrorHelper.exception(e));
			}
		});

		return ResponseEntity.ok().contentType(MediaType.valueOf(type.getContentType())).body(body);
	}
	
	private ReportType getType(String name) {
		try {
			return ReportType.valueOf(name.trim().toUpperCase());
		} catch (Exception e) {
			return ReportType.PDF;
		}
	}
	
	private void export(ReportType type, OutputStream outputStream) {
		// agar report tidak disimpan di memory
		JRSwapFile swapFile = new JRSwapFile(System.getProperty("java.io.tmpdir"), 4096, 100);
		JRSwapFileVirtualizer virtualizer = new JRSwapFileVirtualizer(0, swapFile, true);
		try (BufferedOutputStream bufferedOut = new BufferedOutputStream(outputStream, 8192)) {
			JasperReport report = (JasperReport) JRLoader.loadObject(new ByteArrayInputStream(template));
			List<ReportData> datasource = new ArrayList<>();
			for (long i = 0; i < 100; i++) {
				ReportData data = new ReportData();
				data.setDescription("Deskripsi - " + System.nanoTime());
				data.setName("Name - " + System.nanoTime());
				data.setNumber(i);
				data.setUuid(UUID.randomUUID().toString());
				datasource.add(data);
			}
			ReportInput input = new ReportInput()
			.setType(type)
			.setReport(report)
			.setDatasource(datasource)
			.setParameter(JRParameter.REPORT_VIRTUALIZER, virtualizer)
			.setParameter("MAIN_TITLE", "Contoh Report")
			.setParameter("SUB_TITLE", type.name())
			.setParameter("IMAGE_HEADER", new ByteArrayInputStream(imageHeader))
			.setParameter("IMAGE_DETAIL", new ByteArrayInputStream(imageDetail));
			reportHandler.exportReport(input, bufferedOut);
		} catch (Exception e) {
			throw ErrorHelper.exception(e);
		} finally {
			// Wajib dibersihkan agar file temp di disk terhapus
			virtualizer.cleanup();
		}
	}
	
}
